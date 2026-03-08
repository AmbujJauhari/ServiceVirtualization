import { test, expect, Page, APIRequestContext } from '@playwright/test';
import { JMS_HELPER_URL, uniqueSuffix } from './helpers';

// ============================================================================
// IBM MQ–specific helper
// ============================================================================

interface CreateIbmmqStubOptions {
  name: string;
  destinationType?: 'queue' | 'topic';
  destinationName: string;
  responseDestination?: string;
  responseContent?: string;
  contentMatchType?: 'NONE' | 'CONTAINS' | 'EXACT' | 'REGEX';
  contentPattern?: string;
  messageSelector?: string;
  priority?: number;
  latency?: number;
  status?: 'ACTIVE' | 'INACTIVE';
}

async function createIbmmqStubViaUI(
  page: Page,
  opts: CreateIbmmqStubOptions,
): Promise<void> {
  await page.goto('/ibmmq/stubs/create');

  await page.locator('#name').fill(opts.name);
  await page.locator('#destinationType').selectOption(opts.destinationType ?? 'queue');
  await page.locator('#destinationName').fill(opts.destinationName);

  if (opts.messageSelector) {
    await page.locator('#messageSelector').fill(opts.messageSelector);
  }
  if (opts.priority !== undefined) {
    await page.locator('#priority').fill(String(opts.priority));
  }
  if (opts.contentMatchType && opts.contentMatchType !== 'NONE') {
    await page.locator('#contentMatchType').selectOption(opts.contentMatchType);
    if (opts.contentPattern) {
      await page.locator('#contentPattern').fill(opts.contentPattern);
    }
  }
  if (opts.responseDestination) {
    await page.locator('#responseDestination').fill(opts.responseDestination);
  }
  await page.locator('#responseContent').fill(opts.responseContent ?? 'test response');
  if (opts.latency !== undefined) {
    await page.locator('#latency').fill(String(opts.latency));
  }
  if (opts.status) {
    await page.locator('#status').selectOption(opts.status);
  }

  await Promise.all([
    page.waitForResponse(
      (r) => r.url().includes('/api/ibmmq/stubs') && r.request().method() === 'POST',
      { timeout: 30_000 },
    ),
    page.getByRole('button', { name: 'Create Stub' }).click(),
  ]);
  await page.goto('/ibmmq');
}

function ibmmqRow(page: Page, stubName: string) {
  return page.locator('tbody tr').filter({ hasText: stubName });
}

// ── JMS helper wrappers ────────────────────────────────────────────────────

/** Drain all pending messages from a queue so stale responses don't pollute subsequent tests. */
async function drainQueue(
  request: APIRequestContext,
  queueName: string,
  maxMessages = 30,
): Promise<void> {
  for (let i = 0; i < maxMessages; i++) {
    const res = await request.post(`${JMS_HELPER_URL}/ibmmq/consume`, {
      data: { destinationType: 'QUEUE', destinationName: queueName, timeoutMs: 500 },
    });
    if (res.status() !== 200) break;
    const body = await res.json() as { found: boolean };
    if (!body.found) break;
  }
}

async function jmsPublish(
  request: APIRequestContext,
  opts: {
    destinationType?: string;
    destinationName: string;
    message: string;
    replyTo?: string;
    correlationId?: string;
  },
) {
  const res = await request.post(`${JMS_HELPER_URL}/ibmmq/publish`, {
    data: {
      destinationType: opts.destinationType ?? 'QUEUE',
      destinationName: opts.destinationName,
      message: opts.message,
      replyTo: opts.replyTo,
      correlationId: opts.correlationId,
    },
  });
  expect(res.status(), `IBM MQ publish failed: ${await res.text()}`).toBe(200);
}

async function jmsConsume(
  request: APIRequestContext,
  opts: { destinationName: string; destinationType?: string; timeoutMs?: number },
): Promise<{ found: boolean; message: string | null }> {
  const res = await request.post(`${JMS_HELPER_URL}/ibmmq/consume`, {
    data: {
      destinationType: opts.destinationType ?? 'QUEUE',
      destinationName: opts.destinationName,
      timeoutMs: opts.timeoutMs ?? 6_000,
    },
  });
  expect(res.status(), `IBM MQ consume failed: ${await res.text()}`).toBe(200);
  return res.json();
}

// ============================================================================
// Suite 1 — CRUD lifecycle
// ============================================================================

test.describe('IBM MQ Protocol — stub lifecycle', () => {
  test('stub list page loads and shows the Create Stub button', async ({ page }) => {
    await page.goto('/ibmmq');
    await expect(
      page.getByRole('button', { name: 'Create Stub' }).or(
        page.getByRole('link', { name: 'Create Stub' }),
      ),
    ).toBeVisible();
  });

  test('create a basic queue stub — appears in list with correct destination', async ({
    page,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Basic IBM MQ Queue Stub ${suffix}`;
    const destName = `DEV.QUEUE.${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationType: 'queue',
      destinationName: destName,
      responseContent: `{"status":"ok","id":${suffix}}`,
    });

    const row = ibmmqRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText(destName)).toBeVisible();
    await expect(row.getByText('None')).toBeVisible();
  });

  test('create a topic stub — TOPIC destination type shown in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBM MQ Topic Stub ${suffix}`;
    const destName = `DEV.TOPIC.${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationType: 'topic',
      destinationName: destName,
      responseContent: 'topic response',
    });

    const row = ibmmqRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText('Destination Type: topic', { exact: false })).toBeVisible();
    await expect(row.getByText(destName)).toBeVisible();
  });

  test('create stub with CONTAINS content matching — badge shown in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Contains Stub ${suffix}`;
    const pattern = `correlationId-${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.CONTAINS.${suffix}`,
      contentMatchType: 'CONTAINS',
      contentPattern: pattern,
      responseContent: 'matched contains',
    });

    const row = ibmmqRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText('Contains', { exact: true })).toBeVisible();
    await expect(row.getByText(pattern, { exact: false })).toBeVisible();
  });

  test('create stub with EXACT content matching — badge shown in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Exact Stub ${suffix}`;
    const pattern = `{"requestId":"${suffix}"}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.EXACT.${suffix}`,
      contentMatchType: 'EXACT',
      contentPattern: pattern,
      responseContent: 'matched exact',
    });

    const row = ibmmqRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText('Exact', { exact: true })).toBeVisible();
  });

  test('create stub with REGEX content matching — badge shown in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Regex Stub ${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.REGEX.${suffix}`,
      contentMatchType: 'REGEX',
      contentPattern: `.*orderId.*ORD-\\d+.*`,
      responseContent: 'matched regex',
    });

    const row = ibmmqRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText('Regex', { exact: true })).toBeVisible();
  });

  test('create stub with priority and latency — priority appears in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Priority Stub ${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.PRIO.${suffix}`,
      priority: 10,
      latency: 200,
      responseContent: 'priority response',
    });

    const row = ibmmqRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText('10')).toBeVisible();
  });

  test('create stub with message selector — selector visible in destination cell', async ({
    page,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Selector Stub ${suffix}`;
    const selector = `JMSCorrelationID='CID-${suffix}'`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.SEL.${suffix}`,
      messageSelector: selector,
      responseContent: 'selector response',
    });

    const row = ibmmqRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText(selector, { exact: false })).toBeVisible();
  });

  test('toggle stub status — Active → Inactive → Active', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Toggle Stub ${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.TOGGLE.${suffix}`,
      responseContent: 'toggle test',
    });

    const row = ibmmqRow(page, stubName);
    await expect(row.getByRole('button', { name: 'Active', exact: true })).toBeVisible();

    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/ibmmq/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'Active', exact: true }).click(),
    ]);
    await expect(row.getByRole('button', { name: 'Inactive', exact: true })).toBeVisible({
      timeout: 10_000,
    });

    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/ibmmq/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'Inactive', exact: true }).click(),
    ]);
    await expect(row.getByRole('button', { name: 'Active', exact: true })).toBeVisible({
      timeout: 10_000,
    });
  });

  test('edit stub — updated response content is saved', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Edit Stub ${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.EDIT.${suffix}`,
      responseContent: 'original response',
    });

    const row = ibmmqRow(page, stubName);
    await expect(row).toBeVisible();

    await row.getByRole('link', { name: 'Edit' }).click();
    await page.waitForURL('**/ibmmq/stubs/**/edit');
    await page.locator('#responseContent').fill('updated response');

    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/ibmmq/stubs') && r.request().method() === 'PUT',
        { timeout: 30_000 },
      ),
      page.getByRole('button', { name: 'Update Stub' }).click(),
    ]);
    await page.goto('/ibmmq');

    await ibmmqRow(page, stubName).getByRole('link', { name: 'Edit' }).click();
    await page.waitForURL('**/ibmmq/stubs/**/edit');
    await expect(page.locator('#responseContent')).toHaveValue('updated response');
  });

  test('delete stub — row disappears from list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Delete Stub ${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationName: `DEV.QUEUE.DEL.${suffix}`,
      responseContent: 'to be deleted',
    });

    const row = ibmmqRow(page, stubName);
    await expect(row).toBeVisible();
    await row.scrollIntoViewIfNeeded();

    page.on('dialog', (d) => d.accept());
    await row.getByRole('button', { name: 'Delete', exact: true }).click();

    await expect(row).not.toBeVisible({ timeout: 15_000 });
  });

  test('stub list search filters by stub name', async ({ page }) => {
    const suffix = uniqueSuffix();
    const nameA = `SearchA IBMMQ ${suffix}`;
    const nameB = `SearchB IBMMQ ${suffix}`;

    for (const [name, dest] of [
      [nameA, `DEV.QUEUE.SA.${suffix}`],
      [nameB, `DEV.QUEUE.SB.${suffix}`],
    ] as const) {
      await createIbmmqStubViaUI(page, {
        name,
        destinationName: dest,
        responseContent: 'search test',
      });
    }

    await page.goto('/ibmmq');
    const filterInput = page.getByPlaceholder(
      'Filter stubs by name, description, queue, or content pattern...',
    );
    await filterInput.fill(`SearchA IBMMQ ${suffix}`);

    await expect(ibmmqRow(page, nameA)).toBeVisible();
    await expect(ibmmqRow(page, nameB)).not.toBeVisible();

    await filterInput.clear();
    await expect(ibmmqRow(page, nameA)).toBeVisible();
    await expect(ibmmqRow(page, nameB)).toBeVisible();
  });

  test('create stub set to INACTIVE — status shows Inactive in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Inactive Stub ${suffix}`;

    await page.goto('/ibmmq/stubs/create');
    await page.locator('#name').fill(stubName);
    await page.locator('#destinationType').selectOption('queue');
    await page.locator('#destinationName').fill(`DEV.QUEUE.INACT.${suffix}`);
    await page.locator('#responseContent').fill('inactive response');
    await page.locator('#status').selectOption('INACTIVE');

    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/ibmmq/stubs') && r.request().method() === 'POST',
        { timeout: 30_000 },
      ),
      page.getByRole('button', { name: 'Create Stub' }).click(),
    ]);
    await page.goto('/ibmmq');

    const row = ibmmqRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByRole('button', { name: 'Inactive', exact: true })).toBeVisible();

    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/ibmmq/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'Inactive', exact: true }).click(),
    ]);
    await expect(row.getByRole('button', { name: 'Active', exact: true })).toBeVisible({
      timeout: 10_000,
    });
  });
});

// ============================================================================
// Suite 2 — Message delivery (requires IBM MQ container + jms-helper running)
//
// Prerequisites:
//   docker compose --profile ibmmq up -d
//   cd test/jms-helper && mvn spring-boot:run
//
// The IBM MQ Developer image pre-creates DEV.QUEUE.1, DEV.QUEUE.2, DEV.QUEUE.3.
// Input messages are published to DEV.QUEUE.1.
// Each stub's responseDestination is set to DEV.QUEUE.2 (reply queue).
// Content matching makes each stub unique so they don't interfere when tests
// run sequentially.
// ============================================================================

const INPUT_QUEUE  = 'DEV.QUEUE.1';
const REPLY_QUEUE  = 'DEV.QUEUE.2';

test.describe('IBM MQ Protocol — message delivery', () => {
  test.beforeEach(async ({ request }) => {
    // Drain leftover messages so stale responses from previous runs don't interfere
    await drainQueue(request, REPLY_QUEUE);
    await drainQueue(request, INPUT_QUEUE);
  });

  // --------------------------------------------------------------------------
  // 1. Basic round-trip: publish → stub matches → response received
  // --------------------------------------------------------------------------
  test('basic round-trip — published message triggers stub response', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Delivery Basic ${suffix}`;
    const triggerMsg = `hello-${suffix}`;
    const expectedResponse = `response-for-${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationType: 'queue',
      destinationName: INPUT_QUEUE,
      responseDestination: REPLY_QUEUE,
      contentMatchType: 'CONTAINS',
      contentPattern: triggerMsg,
      responseContent: expectedResponse,
    });
    // Allow the JMS listener container to fully connect before publishing
    await page.waitForTimeout(2500);

    // Publish a message containing the trigger string
    await jmsPublish(request, {
      destinationName: INPUT_QUEUE,
      message: `{"event":"test","payload":"${triggerMsg}"}`,
    });

    // Consume the response that the stub sent to REPLY_QUEUE
    const consumed = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(consumed.found).toBe(true);
    expect(consumed.message).toContain(expectedResponse);
  });

  // --------------------------------------------------------------------------
  // 2. CONTAINS matching: only messages containing the pattern get a response
  // --------------------------------------------------------------------------
  test('CONTAINS matching — non-matching message does not trigger a response', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Contains Delivery ${suffix}`;
    const matchToken = `MATCH-TOKEN-${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationType: 'queue',
      destinationName: INPUT_QUEUE,
      responseDestination: REPLY_QUEUE,
      contentMatchType: 'CONTAINS',
      contentPattern: matchToken,
      responseContent: `matched-${suffix}`,
    });
    await page.waitForTimeout(2500);

    // Publish a message that does NOT contain the token
    await jmsPublish(request, {
      destinationName: INPUT_QUEUE,
      message: `no-match-at-all-${suffix}`,
    });

    // No stub should respond → consume times out
    const noMatch = await jmsConsume(request, { destinationName: REPLY_QUEUE, timeoutMs: 3_000 });
    expect(noMatch.found).toBe(false);

    // Now publish a matching message
    await jmsPublish(request, {
      destinationName: INPUT_QUEUE,
      message: `prefix-${matchToken}-suffix`,
    });

    const match = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(match.found).toBe(true);
    expect(match.message).toContain(`matched-${suffix}`);
  });

  // --------------------------------------------------------------------------
  // 3. EXACT matching
  // --------------------------------------------------------------------------
  test('EXACT matching — only the precise message body triggers a response', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Exact Delivery ${suffix}`;
    const exactBody = `exact-body-${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationType: 'queue',
      destinationName: INPUT_QUEUE,
      responseDestination: REPLY_QUEUE,
      contentMatchType: 'EXACT',
      contentPattern: exactBody,
      responseContent: `exact-response-${suffix}`,
    });
    await page.waitForTimeout(2500);

    // Almost-exact message (extra character) → no response
    await jmsPublish(request, {
      destinationName: INPUT_QUEUE,
      message: `${exactBody}X`,
    });
    const noMatch = await jmsConsume(request, { destinationName: REPLY_QUEUE, timeoutMs: 3_000 });
    expect(noMatch.found).toBe(false);

    // Exact message → response
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: exactBody });
    const match = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(match.found).toBe(true);
    expect(match.message).toContain(`exact-response-${suffix}`);
  });

  // --------------------------------------------------------------------------
  // 4. REGEX matching
  // --------------------------------------------------------------------------
  test('REGEX matching — message matching the pattern triggers a response', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Regex Delivery ${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationType: 'queue',
      destinationName: INPUT_QUEUE,
      responseDestination: REPLY_QUEUE,
      contentMatchType: 'REGEX',
      contentPattern: `.*orderId-${suffix}-\\d+.*`,
      responseContent: `regex-response-${suffix}`,
    });
    await page.waitForTimeout(2500);

    // Non-matching message
    await jmsPublish(request, {
      destinationName: INPUT_QUEUE,
      message: `orderId-${suffix}-ABC`,  // letters, not digits
    });
    const noMatch = await jmsConsume(request, { destinationName: REPLY_QUEUE, timeoutMs: 3_000 });
    expect(noMatch.found).toBe(false);

    // Matching message
    await jmsPublish(request, {
      destinationName: INPUT_QUEUE,
      message: `orderId-${suffix}-12345`,
    });
    const match = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(match.found).toBe(true);
    expect(match.message).toContain(`regex-response-${suffix}`);
  });

  // --------------------------------------------------------------------------
  // 5. INACTIVE stub does not respond; activating it makes it respond
  // --------------------------------------------------------------------------
  test('inactive stub does not respond — activating it makes it respond', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Inactive Delivery ${suffix}`;
    const trigger = `inactive-trigger-${suffix}`;

    // Create stub initially INACTIVE
    await page.goto('/ibmmq/stubs/create');
    await page.locator('#name').fill(stubName);
    await page.locator('#destinationType').selectOption('queue');
    await page.locator('#destinationName').fill(INPUT_QUEUE);
    await page.locator('#responseDestination').fill(REPLY_QUEUE);
    await page.locator('#contentMatchType').selectOption('CONTAINS');
    await page.locator('#contentPattern').fill(trigger);
    await page.locator('#responseContent').fill(`inactive-response-${suffix}`);
    await page.locator('#status').selectOption('INACTIVE');

    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/ibmmq/stubs') && r.request().method() === 'POST',
        { timeout: 30_000 },
      ),
      page.getByRole('button', { name: 'Create Stub' }).click(),
    ]);
    await page.goto('/ibmmq');

    // Publish — stub is INACTIVE so no response
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: trigger });
    const noResponse = await jmsConsume(request, { destinationName: REPLY_QUEUE, timeoutMs: 3_000 });
    expect(noResponse.found).toBe(false);

    // Activate the stub
    const row = ibmmqRow(page, stubName);
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/ibmmq/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'Inactive', exact: true }).click(),
    ]);
    await expect(row.getByRole('button', { name: 'Active', exact: true })).toBeVisible({
      timeout: 10_000,
    });
    // Wait for the newly-registered listener container to connect to IBM MQ
    await page.waitForTimeout(4000);

    // Now publish again — stub is ACTIVE and responds
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: trigger });
    const response = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(response.found).toBe(true);
    expect(response.message).toContain(`inactive-response-${suffix}`);
  });

  // --------------------------------------------------------------------------
  // 6. Deactivating an ACTIVE stub stops responses
  // --------------------------------------------------------------------------
  test('deactivating a stub stops it from responding', async ({ page, request }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Deactivate Delivery ${suffix}`;
    const trigger = `deactivate-trigger-${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationType: 'queue',
      destinationName: INPUT_QUEUE,
      responseDestination: REPLY_QUEUE,
      contentMatchType: 'CONTAINS',
      contentPattern: trigger,
      responseContent: `active-response-${suffix}`,
    });
    await page.waitForTimeout(2500);

    // Confirm it responds while ACTIVE
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: trigger });
    const activeResponse = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(activeResponse.found).toBe(true);
    expect(activeResponse.message).toContain(`active-response-${suffix}`);

    // Deactivate via UI
    const row = ibmmqRow(page, stubName);
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/ibmmq/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'Active', exact: true }).click(),
    ]);
    await expect(row.getByRole('button', { name: 'Inactive', exact: true })).toBeVisible({
      timeout: 10_000,
    });
    // Give the listener container time to fully stop before publishing
    await page.waitForTimeout(2000);

    // Publish again — stub is now INACTIVE, no response
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: trigger });
    const noResponse = await jmsConsume(request, { destinationName: REPLY_QUEUE, timeoutMs: 3_000 });
    expect(noResponse.found).toBe(false);
  });

  // --------------------------------------------------------------------------
  // 7. Edit stub — updated response content is reflected in subsequent messages
  // --------------------------------------------------------------------------
  test('editing response content is reflected in the next message', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `IBMMQ Edit Delivery ${suffix}`;
    const trigger = `edit-trigger-${suffix}`;

    await createIbmmqStubViaUI(page, {
      name: stubName,
      destinationType: 'queue',
      destinationName: INPUT_QUEUE,
      responseDestination: REPLY_QUEUE,
      contentMatchType: 'CONTAINS',
      contentPattern: trigger,
      responseContent: `version-1-${suffix}`,
    });
    await page.waitForTimeout(2500);

    // First message — receives v1 response
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: trigger });
    const v1 = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(v1.found).toBe(true);
    expect(v1.message).toContain(`version-1-${suffix}`);

    // Edit the stub response via UI
    const row = ibmmqRow(page, stubName);
    await row.getByRole('link', { name: 'Edit' }).click();
    await page.waitForURL('**/ibmmq/stubs/**/edit');
    await page.locator('#responseContent').fill(`version-2-${suffix}`);

    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/ibmmq/stubs') && r.request().method() === 'PUT',
        { timeout: 30_000 },
      ),
      page.getByRole('button', { name: 'Update Stub' }).click(),
    ]);
    await page.goto('/ibmmq');

    // Second message — should now receive v2 response
    await jmsPublish(request, { destinationName: INPUT_QUEUE, message: trigger });
    const v2 = await jmsConsume(request, { destinationName: REPLY_QUEUE });
    expect(v2.found).toBe(true);
    expect(v2.message).toContain(`version-2-${suffix}`);
  });
});
