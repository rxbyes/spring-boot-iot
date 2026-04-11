#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { spawn } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const defaultRepoRoot = path.resolve(scriptDir, '..');

function appendLog(logFile, message) {
  fs.appendFileSync(logFile, message, 'utf8');
}

function writeStepLog(logFile, message) {
  const line = `[${new Date().toISOString()}] ${message}\n`;
  process.stdout.write(line);
  appendLog(logFile, line);
}

function pipeChunk(logFile, chunk, writer) {
  const text = chunk.toString();
  writer.write(text);
  appendLog(logFile, text);
}

async function runStep(step, logFile) {
  const [executable, ...args] = step.command;
  writeStepLog(logFile, `START ${step.step}`);

  await new Promise((resolve, reject) => {
    const child = spawn(executable, args, {
      cwd: step.cwd,
      stdio: ['ignore', 'pipe', 'pipe'],
      shell: false
    });

    child.stdout.on('data', (chunk) => pipeChunk(logFile, chunk, process.stdout));
    child.stderr.on('data', (chunk) => pipeChunk(logFile, chunk, process.stderr));
    child.on('error', reject);
    child.on('exit', (code, signal) => {
      if (signal) {
        reject(new Error(`${step.step} exited with signal ${signal}`));
        return;
      }
      if ((code ?? 1) !== 0) {
        reject(new Error(`${step.step} failed with exit code ${code ?? 1}`));
        return;
      }
      resolve();
    });
  });

  writeStepLog(logFile, `PASS ${step.step}`);
}

export function buildGovernanceContractGatePlan({
  repoRoot = defaultRepoRoot,
  platform = process.platform,
  hasMavenSettings = fs.existsSync(path.join(repoRoot, '.mvn', 'settings.xml'))
} = {}) {
  const uiRoot = path.join(repoRoot, 'spring-boot-iot-ui');
  const logFile = path.join(repoRoot, 'logs', 'governance-contract-gates.log');
  const mvnExecutable = platform === 'win32' ? 'mvn.cmd' : 'mvn';
  const npmExecutable = platform === 'win32' ? 'npm.cmd' : 'npm';

  const backendCommand = [
    mvnExecutable,
    ...(hasMavenSettings ? ['-s', path.join(repoRoot, '.mvn', 'settings.xml')] : []),
    '-pl',
    'spring-boot-iot-system,spring-boot-iot-device,spring-boot-iot-alarm',
    '-am',
    '-DskipTests=false',
    '-Dsurefire.failIfNoSpecifiedTests=false',
    '-Dtest=GovernanceApprovalPolicyResolverImplTest,GovernanceApprovalServiceImplTest,ProductModelServiceImplTest,VendorMetricMappingRuntimeServiceImplTest,ProductModelControllerTest,ProductContractReleaseControllerTest,ProductGovernanceApprovalControllerTest,DefaultRiskMetricCatalogPublishRuleTest',
    'test'
  ];

  return {
    repoRoot,
    uiRoot,
    logFile,
    steps: [
      {
        step: 'backend governance contract tests',
        cwd: repoRoot,
        command: backendCommand
      },
      {
        step: 'frontend product governance tests',
        cwd: repoRoot,
        command: [
          npmExecutable,
          '--prefix',
          uiRoot,
          'test',
          '--',
          '--run',
          'src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts'
        ]
      }
    ]
  };
}

export async function runGovernanceContractGates(options = {}) {
  const plan = buildGovernanceContractGatePlan(options);
  fs.mkdirSync(path.dirname(plan.logFile), { recursive: true });
  fs.writeFileSync(plan.logFile, '', 'utf8');
  writeStepLog(plan.logFile, 'Running governance contract quality gates');

  for (const current of plan.steps) {
    await runStep(current, plan.logFile);
  }

  writeStepLog(plan.logFile, 'All governance contract quality gates passed');
}

const isMainModule = process.argv[1]
  ? pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url
  : false;

if (isMainModule) {
  runGovernanceContractGates().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
