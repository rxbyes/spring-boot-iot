import { promises as fs } from "node:fs";
import path from "node:path";
import process from "node:process";

const workspaceRoot = process.cwd();
const viewsRoot = path.join(workspaceRoot, "src", "views");

const governedViews = [
  "ProductWorkbenchView.vue",
  "DeviceWorkbenchView.vue",
  "AuditLogView.vue",
  "MessageTraceView.vue",
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
    {
      pattern: /<StandardWorkbenchRowActions\b/,
      message: "纳管页表格操作列必须使用 StandardWorkbenchRowActions。",
    },
    {
      pattern: /class-name\s*=\s*["'][^"']*\bstandard-row-actions-column\b[^"']*["']/,
      message: '纳管页的“操作”列必须声明 class-name="standard-row-actions-column"。',
    },
    {
      pattern: /resolve(?:Workbench|Adaptive)ActionColumnWidth\(/,
      message: "纳管页的“操作”列必须使用共享自适应列宽解析器。",
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
    {
      pattern: /<StandardRowActions\b[^>]*variant\s*=\s*["']table["']/g,
      message: "纳管页表格操作列禁止直接写 StandardRowActions table 变体，必须走 StandardWorkbenchRowActions。",
    },
    {
      pattern: /label\s*=\s*["']操作["'][\s\S]{0,200}\swidth\s*=\s*["'][^"']+["']/g,
      message: '纳管页“操作”列禁止写死静态 width，必须改为共享自适应列宽。',
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
