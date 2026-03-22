import { promises as fs } from "node:fs";
import path from "node:path";
import process from "node:process";

const workspaceRoot = process.cwd();
const viewsRoot = path.join(workspaceRoot, "src", "views");

const governedViews = [
  "AlarmCenterView.vue",
  "EventDisposalView.vue",
  "RiskPointView.vue",
  "RuleDefinitionView.vue",
  "LinkageRuleView.vue",
  "EmergencyPlanView.vue",
  "OrganizationView.vue",
  "UserView.vue",
  "RoleView.vue",
  "RegionView.vue",
  "MenuView.vue",
  "DictView.vue",
  "ChannelView.vue",
  "InAppMessageView.vue",
  "HelpDocView.vue",
  "AuditLogView.vue",
  "MessageTraceView.vue",
].map((fileName) => path.join(viewsRoot, fileName));

function toRelative(filePath) {
  return path.relative(workspaceRoot, filePath);
}

function getLineNumber(content, index) {
  return content.slice(0, index).split(/\r?\n/).length;
}

function pushError(errors, file, line, message) {
  errors.push({ file, line, message });
}

function scanRequiredUsage(filePath, content, errors) {
  const requirements = [
    {
      pattern: /<StandardListFilterHeader\b/,
      message: "纳管页必须使用 StandardListFilterHeader。",
    },
    {
      pattern: /<StandardTableToolbar\b/,
      message: "纳管页必须使用 StandardTableToolbar。",
    },
    {
      pattern: /<StandardPagination\b/,
      message: "纳管页分页必须使用 StandardPagination。",
    },
  ];

  requirements.forEach(({ pattern, message }) => {
    if (!pattern.test(content)) {
      pushError(errors, filePath, 1, message);
    }
  });
}

function scanForbiddenPatterns(filePath, content, errors) {
  const forbiddenPatterns = [
    {
      pattern: /class\s*=\s*["'][^"']*\bsearch-form\b[^"']*["']/g,
      message: '纳管页禁止继续使用 class="search-form" 作为主筛选模板。',
    },
    {
      pattern: /class\s*=\s*["'][^"']*\btext-right\b[^"']*["']/g,
      message: "纳管页禁止继续使用 text-right 按钮对齐行。",
    },
  ];

  forbiddenPatterns.forEach(({ pattern, message }) => {
    let match = pattern.exec(content);
    while (match) {
      pushError(errors, filePath, getLineNumber(content, match.index), message);
      match = pattern.exec(content);
    }
  });
}

function printErrors(errors) {
  console.error("\nList page guard failed:");
  errors.forEach((error) => {
    console.error(`- ${toRelative(error.file)}:${error.line} ${error.message}`);
  });
}

async function main() {
  const errors = [];

  for (const filePath of governedViews) {
    const content = await fs.readFile(filePath, "utf8");
    scanRequiredUsage(filePath, content, errors);
    scanForbiddenPatterns(filePath, content, errors);
  }

  if (errors.length > 0) {
    printErrors(errors);
    process.exitCode = 1;
    return;
  }

  console.log("List page guard passed.");
}

await main();
