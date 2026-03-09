import { test, expect, Page, APIRequestContext } from '@playwright/test';
import { JMS_HELPER_URL, uniqueSuffix } from './helpers';

// ============================================================================
// Kafka-specific helper
// ============================================================================

interface CreateKafkaStubOptions {
  name: string;
  requestTopic: string;
  responseTopic?: string;
  responseContent?: string;
  contentMatchType?: 'NONE' | 'CONTAINS' | 'EXACT' | 'REGEX';
  contentPattern?: string;
  keyMatchType?: 'NONE' | 'EXACT' | 'CONTAINS' | 'REGEX';
  keyPattern?: string;
  responseKey?: string;
  description?: string;
}

async function createKafkaStubViaUI(
  page: Page,
  opts: CreateKafkaStubOptions,
): Promise<void> {
  await page.goto('/kafka/stubs/create');
  await page.waitForLoadState('domcontentloaded');

  await page.locator('#name').fill(opts.name);

  if (opts.description) {
    await page.locator('#description').fill(opts.description);
  }

  await page.locator('#requestTopic').fill(opts.requestTopic);

  if (opts.keyMatchType && opts.keyMatchType !== 'NONE') {
    await page.locator('#keyMatchType').selectOption(opts.keyMatchType);
    if (opts.keyPattern) {
      await page.locator('#keyPattern').fill(opts.keyPattern);
    }
  }

  if (opts.contentMatchType && opts.contentMatchType !== 'NONE') {
    await page.locator('#contentMatchType').selectOption(opts.contentMatchType);
    if (opts.contentPattern) {
      // contentPattern uses a TextEditor (no id); identify by placeholder text which
      // differs per match type (set after selecting the match type above)
      const placeholderMap: Record<string, string> = {
        CONTAINS: 'Text that should be contained in the message',
        EXACT: 'Complete message content to match exactly',
        REGEX: 'Regular expression pattern to match',
      };
      const placeholder = placeholderMap[opts.contentMatchType];
      await page.getByPlaceholder(placeholder).fill(opts.contentPattern);
    }
  }

  // Direct response tab (default)
  if (opts.responseTopic) {
    await page.locator('#responseTopic').fill(opts.responseTopic);
  }
  if (opts.responseKey) {
    await page.locator('#responseKey').fill(opts.responseKey);
  }

  // Response content (TextEditor – falls back to textarea with same id)
  await page.locator('#responseContent').fill(opts.responseContent ?? 'test kafka response');

  await Promise.all([
    page.waitForResponse(
      (r) => r.url().includes('/api/kafka/stubs') && r.request().method() === 'POST',
      { timeout: 30_000 },
    ),
    page.getByRole('button', { name: 'Create Stub' }).click(),
  ]);
  await page.goto('/kafka');
}

function kafkaRow(page: Page, stubName: string) {
  return page.locator('tbody tr').filter({ hasText: stubName });
}

/** Click the ACTIVE/INACTIVE status button in a row to toggle it. */
async function toggleKafkaStatus(row: ReturnType<typeof kafkaRow>) {
  await row.locator('button').filter({ hasText: /ACTIVE|INACTIVE/ }).click();
}

/** Pause for `ms` milliseconds — gives the backend listener time to process */
const sleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms));

// ── Kafka helper wrappers ─────────────────────────────────────────────────

async function kafkaPublish(
  request: APIRequestContext,
  opts: {
    topic: string;
    message: string;
    key?: string;
    bootstrapServers?: string;
  },
) {
  const res = await request.post(`${JMS_HELPER_URL}/kafka/publish`, {
    data: {
      topic: opts.topic,
      message: opts.message,
      key: opts.key ?? null,
      bootstrapServers: opts.bootstrapServers ?? null,
    },
  });
  expect(res.status(), `Kafka publish failed: ${await res.text()}`).toBe(200);
}

async function kafkaConsume(
  request: APIRequestContext,
  opts: { topic: string; timeoutMs?: number; bootstrapServers?: string },
): Promise<{ found: boolean; message: string | null; key?: string | null }> {
  const res = await request.post(`${JMS_HELPER_URL}/kafka/consume`, {
    data: {
      topic: opts.topic,
      timeoutMs: opts.timeoutMs ?? 8_000,
      bootstrapServers: opts.bootstrapServers ?? null,
    },
  });
  expect(res.status(), `Kafka consume failed: ${await res.text()}`).toBe(200);
  return res.json();
}

// ============================================================================
// Suite 1 — CRUD lifecycle
// ============================================================================

test.describe('Kafka Protocol — stub lifecycle', () => {
  test('stub list page loads and shows the Create Stub button', async ({ page }) => {
    await page.goto('/kafka');
    await expect(
      page.getByRole('button', { name: 'Create Stub' }).or(
        page.getByRole('link', { name: 'Create Stub' }),
      ),
    ).toBeVisible();
  });

  test('create a basic stub — appears in list with correct topic', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Basic Kafka Stub ${suffix}`;
    const topic = `sv.test.in.${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: topic,
      responseContent: `{"status":"ok","id":${suffix}}`,
    });

    const row = kafkaRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText(topic)).toBeVisible();
  });

  test('create stub with CONTAINS content matching — badge shown in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Contains Stub ${suffix}`;
    const pattern = `token-${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: `sv.test.contains.${suffix}`,
      contentMatchType: 'CONTAINS',
      contentPattern: pattern,
      responseContent: 'matched contains',
    });

    const row = kafkaRow(page, stubName);
    await expect(row).toBeVisible();
    // Verify the request topic appears in the row (contentPattern is not shown in the list columns)
    await expect(row.getByText(`sv.test.contains.${suffix}`)).toBeVisible();
  });

  test('create stub with EXACT content matching — appears in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Exact Stub ${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: `sv.test.exact.${suffix}`,
      contentMatchType: 'EXACT',
      contentPattern: `{"requestId":"${suffix}"}`,
      responseContent: 'matched exact',
    });

    const row = kafkaRow(page, stubName);
    await expect(row).toBeVisible();
  });

  test('create stub with REGEX content matching — appears in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Regex Stub ${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: `sv.test.regex.${suffix}`,
      contentMatchType: 'REGEX',
      contentPattern: `.*orderId.*ORD-\\d+.*`,
      responseContent: 'matched regex',
    });

    const row = kafkaRow(page, stubName);
    await expect(row).toBeVisible();
  });

  test('create stub with key matching — appears in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Key Match Stub ${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: `sv.test.key.${suffix}`,
      keyMatchType: 'EXACT',
      keyPattern: `order-key-${suffix}`,
      responseContent: 'key matched response',
    });

    const row = kafkaRow(page, stubName);
    await expect(row).toBeVisible();
  });

  test('create stub with response topic — both topics shown in list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Topic Stub ${suffix}`;
    const inTopic = `sv.test.in.${suffix}`;
    const outTopic = `sv.test.out.${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: inTopic,
      responseTopic: outTopic,
      responseContent: 'topic response',
    });

    const row = kafkaRow(page, stubName);
    await expect(row).toBeVisible();
    await expect(row.getByText(inTopic)).toBeVisible();
    await expect(row.getByText(outTopic)).toBeVisible();
  });

  test('toggle stub status button — ACTIVE to INACTIVE then back', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Toggle Stub ${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: `sv.test.toggle.${suffix}`,
      responseContent: 'toggle test',
    });

    const row = kafkaRow(page, stubName);
    // Stub is created ACTIVE
    await expect(row.locator('button').filter({ hasText: 'ACTIVE' })).toBeVisible();

    // Toggle to INACTIVE
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/kafka/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      toggleKafkaStatus(row),
    ]);
    await expect(row.locator('button').filter({ hasText: 'INACTIVE' })).toBeVisible({ timeout: 10_000 });

    // Toggle back to ACTIVE
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/kafka/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      toggleKafkaStatus(row),
    ]);
    await expect(row.locator('button').filter({ hasText: 'ACTIVE' })).toBeVisible({ timeout: 10_000 });
  });

  test('edit stub — updated response content is persisted', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Edit Stub ${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: `sv.test.edit.${suffix}`,
      responseContent: 'original response',
    });

    const row = kafkaRow(page, stubName);
    await expect(row).toBeVisible();

    await row.getByRole('button', { name: 'Edit' }).click();
    await page.waitForURL('**/kafka/stubs/**/edit');
    await page.locator('#responseContent').fill('updated response');

    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/kafka/stubs') && r.request().method() === 'PUT',
        { timeout: 30_000 },
      ),
      page.getByRole('button', { name: 'Update Stub' }).click(),
    ]);
    await page.goto('/kafka');

    await kafkaRow(page, stubName).getByRole('button', { name: 'Edit' }).click();
    await page.waitForURL('**/kafka/stubs/**/edit');
    await expect(page.locator('#responseContent')).toHaveValue('updated response');
  });

  test('delete stub — row disappears from list', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Delete Stub ${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: `sv.test.del.${suffix}`,
      responseContent: 'to be deleted',
    });

    const row = kafkaRow(page, stubName);
    await expect(row).toBeVisible();

    page.on('dialog', (d) => d.accept());
    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/kafka/stubs') && r.request().method() === 'DELETE',
        { timeout: 10_000 },
      ),
      row.getByRole('button', { name: 'Delete', exact: true }).click(),
    ]);

    await expect(row).not.toBeVisible({ timeout: 10_000 });
  });

  test('stub list search filters by stub name', async ({ page }) => {
    const suffix = uniqueSuffix();
    const nameA = `KafkaSearchA ${suffix}`;
    const nameB = `KafkaSearchB ${suffix}`;

    for (const [name, topic] of [
      [nameA, `sv.test.sa.${suffix}`],
      [nameB, `sv.test.sb.${suffix}`],
    ] as const) {
      await createKafkaStubViaUI(page, { name, requestTopic: topic, responseContent: 'search test' });
    }

    await page.goto('/kafka');
    const filterInput = page.getByPlaceholder('Search stubs by name, description, topic, or content matcher...');
    await filterInput.fill(`KafkaSearchA ${suffix}`);

    await expect(kafkaRow(page, nameA)).toBeVisible();
    await expect(kafkaRow(page, nameB)).not.toBeVisible();

    await filterInput.fill('');
    await expect(kafkaRow(page, nameA)).toBeVisible();
    await expect(kafkaRow(page, nameB)).toBeVisible();
  });

  test('stub shows INACTIVE status when toggled off', async ({ page }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Inactive Stub ${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: `sv.test.inact.${suffix}`,
      responseContent: 'inactive response',
    });

    const row = kafkaRow(page, stubName);
    await expect(row).toBeVisible();
    // Created as ACTIVE by default
    await expect(row.locator('button').filter({ hasText: 'ACTIVE' })).toBeVisible();

    // Toggle to INACTIVE
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/kafka/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      toggleKafkaStatus(row),
    ]);
    await expect(row.locator('button').filter({ hasText: 'INACTIVE' })).toBeVisible({ timeout: 10_000 });

    // Can be re-activated
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/kafka/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      toggleKafkaStatus(row),
    ]);
    await expect(row.locator('button').filter({ hasText: 'ACTIVE' })).toBeVisible({ timeout: 10_000 });
  });
});

// ============================================================================
// Suite 2 — Message delivery (requires Kafka container + jms-helper running)
//
// Prerequisites:
//   docker compose --profile kafka up -d   (kafka broker on localhost:9092)
//   cd test/jms-helper && mvn spring-boot:run
//
// Each test uses unique topics to avoid cross-test interference.
// The backend auto-creates topics and registers dynamic consumers per stub.
// ============================================================================

test.describe('Kafka Protocol — message delivery', () => {
  // --------------------------------------------------------------------------
  // 1. Basic round-trip
  // --------------------------------------------------------------------------
  test('basic round-trip — published message triggers stub response', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Delivery Basic ${suffix}`;
    const inTopic = `sv.delivery.in.${suffix}`;
    const outTopic = `sv.delivery.out.${suffix}`;
    const triggerMsg = `hello-${suffix}`;
    const expectedResponse = `response-for-${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: inTopic,
      responseTopic: outTopic,
      contentMatchType: 'CONTAINS',
      contentPattern: triggerMsg,
      responseContent: expectedResponse,
    });

    await kafkaPublish(request, {
      topic: inTopic,
      message: `{"event":"test","payload":"${triggerMsg}"}`,
    });
    await sleep(2_000); // allow listener to process

    const consumed = await kafkaConsume(request, { topic: outTopic, timeoutMs: 25_000 });
    expect(consumed.found).toBe(true);
    expect(consumed.message).toContain(expectedResponse);
  });

  // --------------------------------------------------------------------------
  // 2. CONTAINS matching
  // --------------------------------------------------------------------------
  test('CONTAINS matching — non-matching message does not trigger a response', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Contains Delivery ${suffix}`;
    const inTopic = `sv.delivery.in.${suffix}`;
    const outTopic = `sv.delivery.out.${suffix}`;
    const matchToken = `MATCH-TOKEN-${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: inTopic,
      responseTopic: outTopic,
      contentMatchType: 'CONTAINS',
      contentPattern: matchToken,
      responseContent: `matched-${suffix}`,
    });

    // Non-matching message → no response
    await kafkaPublish(request, { topic: inTopic, message: `no-match-${suffix}` });
    await sleep(3_000); // let stub process (should produce nothing)
    const noMatch = await kafkaConsume(request, { topic: outTopic, timeoutMs: 3_000 });
    expect(noMatch.found).toBe(false);

    // Matching message → response
    await kafkaPublish(request, { topic: inTopic, message: `prefix-${matchToken}-suffix` });
    await sleep(2_000); // allow listener to process
    const match = await kafkaConsume(request, { topic: outTopic, timeoutMs: 25_000 });
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
    const stubName = `Kafka Exact Delivery ${suffix}`;
    const inTopic = `sv.delivery.in.${suffix}`;
    const outTopic = `sv.delivery.out.${suffix}`;
    const exactBody = `exact-body-${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: inTopic,
      responseTopic: outTopic,
      contentMatchType: 'EXACT',
      contentPattern: exactBody,
      responseContent: `exact-response-${suffix}`,
    });

    // Near-exact body → no response
    await kafkaPublish(request, { topic: inTopic, message: `${exactBody}X` });
    await sleep(3_000); // let stub process (should produce nothing)
    const noMatch = await kafkaConsume(request, { topic: outTopic, timeoutMs: 3_000 });
    expect(noMatch.found).toBe(false);

    // Exact body → response
    await kafkaPublish(request, { topic: inTopic, message: exactBody });
    await sleep(2_000); // allow listener to process
    const match = await kafkaConsume(request, { topic: outTopic, timeoutMs: 25_000 });
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
    const stubName = `Kafka Regex Delivery ${suffix}`;
    const inTopic = `sv.delivery.in.${suffix}`;
    const outTopic = `sv.delivery.out.${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: inTopic,
      responseTopic: outTopic,
      contentMatchType: 'REGEX',
      contentPattern: `.*orderId-${suffix}-\\d+.*`,
      responseContent: `regex-response-${suffix}`,
    });

    // Non-matching (letters not digits)
    await kafkaPublish(request, { topic: inTopic, message: `orderId-${suffix}-ABC` });
    await sleep(3_000); // let stub process (should produce nothing)
    const noMatch = await kafkaConsume(request, { topic: outTopic, timeoutMs: 3_000 });
    expect(noMatch.found).toBe(false);

    // Matching
    await kafkaPublish(request, { topic: inTopic, message: `orderId-${suffix}-99887` });
    await sleep(2_000); // allow listener to process
    const match = await kafkaConsume(request, { topic: outTopic, timeoutMs: 25_000 });
    expect(match.found).toBe(true);
    expect(match.message).toContain(`regex-response-${suffix}`);
  });

  // --------------------------------------------------------------------------
  // 5. Key matching — stub only responds when key matches
  // --------------------------------------------------------------------------
  test('key matching — stub responds only to messages with matching key', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Key Delivery ${suffix}`;
    const inTopic = `sv.delivery.in.${suffix}`;
    const outTopic = `sv.delivery.out.${suffix}`;
    const expectedKey = `order-${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: inTopic,
      responseTopic: outTopic,
      keyMatchType: 'EXACT',
      keyPattern: expectedKey,
      responseContent: `key-matched-response-${suffix}`,
    });

    // Wrong key → no response
    await kafkaPublish(request, {
      topic: inTopic,
      message: `some message`,
      key: `wrong-key-${suffix}`,
    });
    await sleep(3_000); // let stub process (should produce nothing)
    const noMatch = await kafkaConsume(request, { topic: outTopic, timeoutMs: 3_000 });
    expect(noMatch.found).toBe(false);

    // Correct key → response
    await kafkaPublish(request, {
      topic: inTopic,
      message: `some message`,
      key: expectedKey,
    });
    await sleep(2_000); // allow listener to process
    const match = await kafkaConsume(request, { topic: outTopic, timeoutMs: 25_000 });
    expect(match.found).toBe(true);
    expect(match.message).toContain(`key-matched-response-${suffix}`);
  });

  // --------------------------------------------------------------------------
  // 6. INACTIVE stub does not respond; activating it makes it respond
  // --------------------------------------------------------------------------
  test('inactive stub does not respond — activating it makes it respond', async ({
    page,
    request,
  }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Inactive Delivery ${suffix}`;
    const inTopic = `sv.delivery.in.${suffix}`;
    const outTopic = `sv.delivery.out.${suffix}`;
    const trigger = `inactive-trigger-${suffix}`;

    // Create stub (ACTIVE) then immediately toggle to INACTIVE
    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: inTopic,
      responseTopic: outTopic,
      contentMatchType: 'CONTAINS',
      contentPattern: trigger,
      responseContent: `inactive-response-${suffix}`,
    });

    const row = kafkaRow(page, stubName);
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/kafka/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      toggleKafkaStatus(row),
    ]);
    await expect(row.locator('button').filter({ hasText: 'INACTIVE' })).toBeVisible({ timeout: 10_000 });

    // Stub INACTIVE — no response
    await kafkaPublish(request, { topic: inTopic, message: trigger });
    await sleep(3_000); // let stub process (should produce nothing since INACTIVE)
    const noResponse = await kafkaConsume(request, { topic: outTopic, timeoutMs: 3_000 });
    expect(noResponse.found).toBe(false);

    // Activate the stub
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/kafka/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      toggleKafkaStatus(row),
    ]);
    await expect(row.locator('button').filter({ hasText: 'ACTIVE' })).toBeVisible({ timeout: 10_000 });

    // Now publishes get a response
    await kafkaPublish(request, { topic: inTopic, message: trigger });
    await sleep(2_000); // allow listener to process
    const response = await kafkaConsume(request, { topic: outTopic, timeoutMs: 25_000 });
    expect(response.found).toBe(true);
    expect(response.message).toContain(`inactive-response-${suffix}`);
  });

  // --------------------------------------------------------------------------
  // 7. Deactivating a stub stops it from responding
  //
  // Strategy: create stub, toggle INACTIVE immediately (before any publish),
  // then publish and consume on a fresh unique topic. No prior messages exist
  // on the outTopic so found=false proves the stub didn't respond.
  // --------------------------------------------------------------------------
  test('deactivating a stub stops it from responding', async ({ page, request }) => {
    const suffix = uniqueSuffix();
    const stubName = `Kafka Deactivate Delivery ${suffix}`;
    const inTopic = `sv.delivery.in.${suffix}`;
    const outTopic = `sv.delivery.out.${suffix}`;
    const trigger = `deactivate-trigger-${suffix}`;

    await createKafkaStubViaUI(page, {
      name: stubName,
      requestTopic: inTopic,
      responseTopic: outTopic,
      contentMatchType: 'CONTAINS',
      contentPattern: trigger,
      responseContent: `active-response-${suffix}`,
    });

    // Deactivate immediately (before any publish so outTopic is empty)
    const row = kafkaRow(page, stubName);
    await Promise.all([
      page.waitForResponse(
        (r) =>
          r.url().includes('/kafka/stubs') &&
          r.request().method() === 'PATCH' &&
          r.status() === 200,
        { timeout: 10_000 },
      ),
      toggleKafkaStatus(row),
    ]);
    await expect(row.locator('button').filter({ hasText: 'INACTIVE' })).toBeVisible({ timeout: 10_000 });

    // Stub INACTIVE — publish but expect no response on the fresh outTopic
    await kafkaPublish(request, { topic: inTopic, message: trigger });
    await sleep(3_000); // let stub process (should produce nothing since INACTIVE)
    const noResponse = await kafkaConsume(request, { topic: outTopic, timeoutMs: 3_000 });
    expect(noResponse.found).toBe(false);
  });
});
