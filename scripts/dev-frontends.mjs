import { spawn } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { createInterface } from 'node:readline';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.resolve(scriptDir, '..');
const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';
const dryRun = process.argv.includes('--dry-run');
const resetColor = '\x1b[0m';
const frontendDir = 'PetMeet-frontend';

const projects = [
  { label: 'admin', dir: path.join(frontendDir, 'PetMeet-admin'), color: '\x1b[36m' },
  { label: 'user ', dir: path.join(frontendDir, 'PetMeet-user'), color: '\x1b[32m' }
];

for (const project of projects) {
  const packageJsonPath = path.join(rootDir, project.dir, 'package.json');
  if (!fs.existsSync(packageJsonPath)) {
    console.error(`Missing package.json: ${packageJsonPath}`);
    process.exit(1);
  }
}

if (dryRun) {
  for (const project of projects) {
    console.log(`${project.label.trim()}: ${path.join(rootDir, project.dir)}`);
  }
  process.exit(0);
}

const children = new Map();
let shuttingDown = false;

function writeLine(project, line, stream = process.stdout) {
  if (!line) {
    return;
  }

  stream.write(`${project.color}[${project.label}]${resetColor} ${line}\n`);
}

function forwardOutput(project, stream, target) {
  const reader = createInterface({ input: stream });
  reader.on('line', (line) => writeLine(project, line, target));
}

function stopChild(child) {
  if (child.exitCode !== null || child.signalCode !== null) {
    return Promise.resolve();
  }

  if (process.platform === 'win32') {
    return new Promise((resolve) => {
      const killer = spawn('taskkill', ['/pid', String(child.pid), '/T', '/F'], {
        stdio: 'ignore'
      });
      killer.on('exit', () => resolve());
      killer.on('error', () => resolve());
    });
  }

  child.kill('SIGTERM');
  return Promise.resolve();
}

async function shutdown(message, exitCode) {
  if (shuttingDown) {
    return;
  }

  shuttingDown = true;

  if (message) {
    console.log(message);
  }

  await Promise.all([...children.values()].map(({ child }) => stopChild(child)));
  process.exit(exitCode);
}

for (const project of projects) {
  const cwd = path.join(rootDir, project.dir);
  const child = spawn(npmCommand, ['run', 'dev'], {
    cwd,
    env: process.env,
    shell: false,
    stdio: ['inherit', 'pipe', 'pipe']
  });

  children.set(project.label, { child, project });

  forwardOutput(project, child.stdout, process.stdout);
  forwardOutput(project, child.stderr, process.stderr);

  child.on('error', (error) => {
    shutdown(`${project.label.trim()} failed to start: ${error.message}`, 1);
  });

  child.on('exit', (code, signal) => {
    if (shuttingDown) {
      return;
    }

    const details = signal
      ? `signal ${signal}`
      : `code ${code ?? 'unknown'}`;
    shutdown(`${project.label.trim()} exited with ${details}.`, code === 0 ? 0 : 1);
  });
}

process.on('SIGINT', () => shutdown('Stopping admin and user dev servers...', 0));
process.on('SIGTERM', () => shutdown('Stopping admin and user dev servers...', 0));
