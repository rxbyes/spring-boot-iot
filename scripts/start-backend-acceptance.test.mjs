import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawn } from 'node:child_process';

function runPowerShellScript(scriptPath, env = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(
      'powershell',
      ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', scriptPath],
      {
        cwd: path.dirname(path.dirname(scriptPath)),
        env: {
          ...process.env,
          ...env
        },
        stdio: ['ignore', 'pipe', 'pipe']
      }
    );
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (chunk) => {
      stdout += chunk.toString();
    });
    child.stderr.on('data', (chunk) => {
      stderr += chunk.toString();
    });
    child.on('error', reject);
    child.on('close', (code) => {
      resolve({
        status: code ?? 1,
        stdout,
        stderr
      });
    });
  });
}

function createFakeCommand(rootDir, commandName, lines) {
  const filePath = path.join(rootDir, commandName);
  fs.writeFileSync(filePath, `${lines.join('\r\n')}\r\n`, 'utf8');
  return filePath;
}

test('backend acceptance script tolerates java stderr warnings when exit code is zero', async () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'backend-acceptance-'));
  const scriptsDir = path.join(tempRoot, 'scripts');
  const adminTargetDir = path.join(tempRoot, 'spring-boot-iot-admin', 'target');
  const fakeBinDir = path.join(tempRoot, 'fake-bin');
  const sourceScriptPath = path.resolve('scripts/start-backend-acceptance.ps1');
  const tempScriptPath = path.join(scriptsDir, 'start-backend-acceptance.ps1');
  const jarPath = path.join(adminTargetDir, 'spring-boot-iot-admin-1.0.0-SNAPSHOT.jar');
  const javaArgsPath = path.join(tempRoot, 'java-args.txt');

  fs.mkdirSync(scriptsDir, { recursive: true });
  fs.mkdirSync(adminTargetDir, { recursive: true });
  fs.mkdirSync(fakeBinDir, { recursive: true });
  fs.copyFileSync(sourceScriptPath, tempScriptPath);
  fs.writeFileSync(jarPath, 'fake-jar', 'utf8');

  createFakeCommand(fakeBinDir, 'mvn.cmd', [
    '@echo off',
    'echo fake maven build',
    'exit /b 0'
  ]);
  createFakeCommand(fakeBinDir, 'java.cmd', [
    '@echo off',
    'echo Missing watchable .xml or .properties files. 1>&2',
    `set args=%*`,
    `> "${javaArgsPath}" echo %args%`,
    'exit /b 0'
  ]);

  const result = await runPowerShellScript(tempScriptPath, {
    PATH: `${fakeBinDir}${path.delimiter}${process.env.PATH ?? ''}`
  });
  const javaArgs = fs.readFileSync(javaArgsPath, 'utf8');

  fs.rmSync(tempRoot, { recursive: true, force: true });

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.match(result.stdout, /starting backend with profile dev/i);
  assert.match(javaArgs, /--spring\.profiles\.active=dev/);
});

test('backend acceptance script forwards optional backend port override to java arguments', async () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'backend-acceptance-port-'));
  const scriptsDir = path.join(tempRoot, 'scripts');
  const adminTargetDir = path.join(tempRoot, 'spring-boot-iot-admin', 'target');
  const fakeBinDir = path.join(tempRoot, 'fake-bin');
  const sourceScriptPath = path.resolve('scripts/start-backend-acceptance.ps1');
  const tempScriptPath = path.join(scriptsDir, 'start-backend-acceptance.ps1');
  const jarPath = path.join(adminTargetDir, 'spring-boot-iot-admin-1.0.0-SNAPSHOT.jar');
  const javaArgsPath = path.join(tempRoot, 'java-args.txt');

  fs.mkdirSync(scriptsDir, { recursive: true });
  fs.mkdirSync(adminTargetDir, { recursive: true });
  fs.mkdirSync(fakeBinDir, { recursive: true });
  fs.copyFileSync(sourceScriptPath, tempScriptPath);
  fs.writeFileSync(jarPath, 'fake-jar', 'utf8');

  createFakeCommand(fakeBinDir, 'mvn.cmd', [
    '@echo off',
    'echo fake maven build',
    'exit /b 0'
  ]);
  createFakeCommand(fakeBinDir, 'java.cmd', [
    '@echo off',
    `set args=%*`,
    `> "${javaArgsPath}" echo %args%`,
    'exit /b 0'
  ]);

  const result = await runPowerShellScript(tempScriptPath, {
    IOT_BACKEND_ACCEPTANCE_PORT: '10099',
    PATH: `${fakeBinDir}${path.delimiter}${process.env.PATH ?? ''}`
  });

  const javaArgs = fs.readFileSync(javaArgsPath, 'utf8');

  fs.rmSync(tempRoot, { recursive: true, force: true });

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.match(javaArgs, /--server\.port=10099/);
});

test('backend acceptance script launches a copied runtime jar instead of the target artifact', async () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'backend-acceptance-runtime-'));
  const scriptsDir = path.join(tempRoot, 'scripts');
  const adminTargetDir = path.join(tempRoot, 'spring-boot-iot-admin', 'target');
  const fakeBinDir = path.join(tempRoot, 'fake-bin');
  const sourceScriptPath = path.resolve('scripts/start-backend-acceptance.ps1');
  const tempScriptPath = path.join(scriptsDir, 'start-backend-acceptance.ps1');
  const targetJarPath = path.join(adminTargetDir, 'spring-boot-iot-admin-1.0.0-SNAPSHOT.jar');
  const javaArgsPath = path.join(tempRoot, 'java-args.txt');

  fs.mkdirSync(scriptsDir, { recursive: true });
  fs.mkdirSync(adminTargetDir, { recursive: true });
  fs.mkdirSync(fakeBinDir, { recursive: true });
  fs.copyFileSync(sourceScriptPath, tempScriptPath);
  fs.writeFileSync(targetJarPath, 'fake-jar', 'utf8');

  createFakeCommand(fakeBinDir, 'mvn.cmd', [
    '@echo off',
    'echo fake maven build',
    'exit /b 0'
  ]);
  createFakeCommand(fakeBinDir, 'java.cmd', [
    '@echo off',
    `set args=%*`,
    `> "${javaArgsPath}" echo %args%`,
    'exit /b 0'
  ]);

  const result = await runPowerShellScript(tempScriptPath, {
    PATH: `${fakeBinDir}${path.delimiter}${process.env.PATH ?? ''}`
  });

  const javaArgs = fs.readFileSync(javaArgsPath, 'utf8');
  const runtimeDir = path.join(tempRoot, 'logs', 'backend-runtime');
  const runtimeEntries = fs.existsSync(runtimeDir) ? fs.readdirSync(runtimeDir) : [];

  fs.rmSync(tempRoot, { recursive: true, force: true });

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.doesNotMatch(
    javaArgs,
    /spring-boot-iot-admin\\target\\spring-boot-iot-admin-1\.0\.0-SNAPSHOT\.jar/
  );
  assert.ok(runtimeEntries.length > 0, 'expected a copied runtime jar to be created');
});
