import test from 'node:test';
import assert from 'node:assert/strict';

import { createConfigDrivenScenarios } from './auto/browser-config-driven.mjs';

class FakeLocator {
  constructor(state = {}) {
    this.value = state.value ?? '';
    this.checked = state.checked;
    this.disabled = state.disabled;
  }

  first() {
    return this;
  }

  locator() {
    return this;
  }

  async inputValue() {
    return this.value;
  }

  async getAttribute(name) {
    if (name === 'value') {
      return this.value;
    }
    if (name === 'aria-checked' && typeof this.checked === 'boolean') {
      return String(this.checked);
    }
    if (name === 'disabled' && this.disabled) {
      return '';
    }
    if (name === 'aria-disabled' && typeof this.disabled === 'boolean') {
      return String(this.disabled);
    }
    return null;
  }

  async isChecked() {
    if (typeof this.checked !== 'boolean') {
      throw new Error('locator is not checkable');
    }
    return this.checked;
  }

  async isDisabled() {
    if (typeof this.disabled !== 'boolean') {
      throw new Error('locator does not expose disabled state');
    }
    return this.disabled;
  }

  async check() {
    this.checked = true;
  }

  async uncheck() {
    this.checked = false;
  }

  async click() {
    if (typeof this.checked === 'boolean') {
      this.checked = !this.checked;
    }
  }
}

class FakePage {
  constructor(locators = {}, options = {}) {
    this.locators = locators;
    this.currentUrl = options.url || 'http://127.0.0.1:5175/protocol-governance';
    this.onWaitForURL = options.onWaitForURL;
  }

  getByTestId(testId) {
    return new FakeLocator(this.locators[testId]);
  }

  locator(selector) {
    return new FakeLocator(this.locators[selector]);
  }

  url() {
    return this.currentUrl;
  }

  async waitForURL(predicate) {
    if (typeof this.onWaitForURL === 'function') {
      await this.onWaitForURL(this);
    }
    if (!predicate(new URL(this.currentUrl))) {
      throw new Error(`Expected URL predicate to match "${this.currentUrl}".`);
    }
  }
}

function buildPlan(step) {
  return {
    target: {
      planName: 'step handler test',
      frontendBaseUrl: 'http://127.0.0.1:5175',
      backendBaseUrl: 'http://127.0.0.1:10099'
    },
    scenarios: [
      {
        key: 'step-handler',
        name: 'step handler',
        route: '/protocol-governance',
        readySelector: '',
        requiresLogin: false,
        steps: [step]
      }
    ]
  };
}

function createScenario(step, runToken = 'token-1') {
  const [scenario] = createConfigDrivenScenarios(buildPlan(step))({ runToken });
  return scenario;
}

test('assertValue step passes when input value matches expected text', async () => {
  const scenario = createScenario({
    id: 'assert-input-value',
    label: 'assert input value',
    type: 'assertValue',
    locator: {
      type: 'testId',
      value: 'protocol-family-display-name'
    },
    value: 'automated family'
  });

  const detail = await scenario.run({
    page: new FakePage({
      'protocol-family-display-name': {
        value: 'automated family'
      }
    }),
    runtime: {},
    helpers: {
      openRoute: async () => []
    },
    options: {}
  });

  assert.equal(detail.stepResults[0].status, 'passed');
  assert.equal(detail.stepResults[0].expected, 'automated family');
  assert.equal(detail.stepResults[0].actual, 'automated family');
});

test('assertValue step fails when input value does not match expected text', async () => {
  const scenario = createScenario(
    {
      id: 'assert-input-value',
      label: 'assert input value',
      type: 'assertValue',
      locator: {
        type: 'testId',
        value: 'protocol-family-display-name'
      },
      value: 'automated family'
    },
    'token-2'
  );

  await assert.rejects(
    () =>
      scenario.run({
        page: new FakePage({
          'protocol-family-display-name': {
            value: 'temporary value'
          }
        }),
        runtime: {},
        helpers: {
          openRoute: async () => []
        },
        options: {}
      }),
    /Expected value "automated family", got "temporary value"\./
  );
});

test('setChecked step accepts boolean false value and unchecks the locator', async () => {
  const scenario = createScenario(
    {
      id: 'set-checkbox-false',
      label: 'set checkbox false',
      type: 'setChecked',
      locator: {
        type: 'testId',
        value: 'family-checkbox'
      },
      value: false
    },
    'token-3'
  );

  const detail = await scenario.run({
    page: new FakePage({
      'family-checkbox': {
        checked: true
      }
    }),
    runtime: {},
    helpers: {
      openRoute: async () => []
    },
    options: {}
  });

  assert.equal(detail.stepResults[0].status, 'passed');
  assert.equal(detail.stepResults[0].checked, false);
});

test('assertDisabled step passes when the locator is disabled', async () => {
  const scenario = createScenario(
    {
      id: 'assert-disabled',
      label: 'assert disabled',
      type: 'assertDisabled',
      locator: {
        type: 'testId',
        value: 'profile-batch-rollback'
      }
    },
    'token-4'
  );

  const detail = await scenario.run({
    page: new FakePage({
      'profile-batch-rollback': {
        disabled: true
      }
    }),
    runtime: {},
    helpers: {
      openRoute: async () => []
    },
    options: {}
  });

  assert.equal(detail.stepResults[0].status, 'passed');
  assert.equal(detail.stepResults[0].disabled, true);
});

test('assertPathnameEquals waits for the route redirect when timeout is provided', async () => {
  const scenario = createScenario(
    {
      id: 'assert-quality-workbench-pathname',
      label: 'assert quality workbench pathname',
      type: 'assertPathnameEquals',
      value: '/quality-workbench',
      timeout: 1000
    },
    'token-5'
  );

  const detail = await scenario.run({
    page: new FakePage(
      {},
      {
        url: 'http://127.0.0.1:5175/login?redirect=/quality-workbench',
        onWaitForURL: async (page) => {
          page.currentUrl = 'http://127.0.0.1:5175/quality-workbench';
        }
      }
    ),
    runtime: {},
    helpers: {
      openRoute: async () => []
    },
    options: {}
  });

  assert.equal(detail.stepResults[0].status, 'passed');
  assert.equal(detail.stepResults[0].expected, '/quality-workbench');
  assert.equal(detail.stepResults[0].actual, '/quality-workbench');
});
