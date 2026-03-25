#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { spawn } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const powershellScript = path.join(scriptDir, 'run-quality-gates.ps1');
const shellScript = path.join(scriptDir, 'run-quality-gates.sh');

function listPathEntries(envPath) {
  if (!envPath) {
    return [];
  }
  return envPath.split(path.delimiter).filter(Boolean);
}

function fileExists(filePath) {
  try {
    fs.accessSync(filePath, fs.constants.X_OK);
    return true;
  } catch {
    return false;
  }
}

function hasCommand(command, platform = process.platform, envPath = process.env.PATH ?? '') {
  const entries = listPathEntries(envPath);
  if (entries.length === 0) {
    return false;
  }

  const extensions = platform === 'win32'
    ? (process.env.PATHEXT || '.EXE;.CMD;.BAT;.COM')
        .split(';')
        .filter(Boolean)
    : [''];

  for (const entry of entries) {
    for (const extension of extensions) {
      const candidatePath = path.join(entry, platform === 'win32' ? `${command}${extension}` : command);
      if (fileExists(candidatePath)) {
        return true;
      }
    }
  }

  return false;
}

export function resolveQualityGateRunner({
  platform = process.platform,
  availableCommands
} = {}) {
  const commands = availableCommands
    ? new Set(availableCommands)
    : new Set(
        ['pwsh', 'powershell', 'bash', 'sh'].filter((command) => hasCommand(command, platform))
      );

  if (platform === 'win32') {
    const executable = commands.has('pwsh')
      ? 'pwsh'
      : commands.has('powershell')
        ? 'powershell'
        : null;
    if (!executable) {
      throw new Error('PowerShell executable was not found in PATH.');
    }
    return {
      executable,
      scriptPath: powershellScript,
      args: ['-ExecutionPolicy', 'Bypass', '-File', powershellScript]
    };
  }

  const executable = commands.has('bash')
    ? 'bash'
    : commands.has('sh')
      ? 'sh'
      : null;
  if (!executable) {
    throw new Error('Shell executable was not found in PATH.');
  }
  return {
    executable,
    scriptPath: shellScript,
    args: [shellScript]
  };
}

export async function runQualityGates() {
  const runner = resolveQualityGateRunner();
  await new Promise((resolve, reject) => {
    const child = spawn(runner.executable, runner.args, {
      stdio: 'inherit'
    });

    child.on('error', reject);
    child.on('exit', (code, signal) => {
      if (signal) {
        reject(new Error(`Quality gates process exited with signal ${signal}`));
        return;
      }
      if ((code ?? 1) !== 0) {
        process.exitCode = code ?? 1;
      }
      resolve();
    });
  });
}

const isMainModule = process.argv[1]
  ? pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url
  : false;

if (isMainModule) {
  runQualityGates().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
