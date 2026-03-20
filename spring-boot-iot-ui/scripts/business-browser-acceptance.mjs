import process from 'node:process';

import { runCli } from '../../scripts/auto/run-browser-acceptance.mjs';

await runCli(process.argv.slice(2));
