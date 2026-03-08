import { test, expect } from '@playwright/test';

const EXPECTED_PROTOCOLS = [
  // Sourced from the health API (may be enabled or disabled depending on active Spring profiles)
  'REST',
  'SOAP',
  'TIBCO EMS',
  'IBM MQ',
  'Kafka',
  'ActiveMQ',
  // Hardcoded additionalServices — always present regardless of backend state
  'File Service',
  'File Management',
  'About Platform',
];

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    // Wait for the loading spinner to disappear before asserting
    await expect(page.getByText('Loading protocol status...')).not.toBeVisible({ timeout: 15_000 });
  });

  test('shows all 9 protocol cards', async ({ page }) => {
    // Each card has an h2 with the protocol name
    for (const name of EXPECTED_PROTOCOLS) {
      await expect(
        page.locator('h2').filter({ hasText: name }),
        `Expected card for "${name}" to be visible`
      ).toBeVisible();
    }
  });

  test('shows exactly 9 protocol cards', async ({ page }) => {
    // Cards are identified by the shadow-md class applied to every card container
    await expect(page.locator('.shadow-md')).toHaveCount(9);
  });

  test('REST and SOAP cards are always enabled', async ({ page }) => {
    // REST and SOAP are marked alwaysEnabled in protocolConfig — they must never show Disabled
    for (const name of ['REST', 'SOAP']) {
      const card = page.locator('.shadow-md').filter({ has: page.locator('h2', { hasText: name }) });
      await expect(card.getByText('Enabled'), `"${name}" should show Enabled badge`).toBeVisible();
      await expect(card.getByText('Disabled'), `"${name}" should not show Disabled badge`).not.toBeVisible();
    }
  });

  test('each enabled card has a Manage link', async ({ page }) => {
    // Every card that shows the "Enabled" badge should have a "Manage" link, not "Unavailable"
    const cards = page.locator('.shadow-md');
    const count = await cards.count();

    for (let i = 0; i < count; i++) {
      const card = cards.nth(i);
      const isEnabled = await card.getByText('Enabled').isVisible();
      if (isEnabled) {
        await expect(
          card.getByRole('link', { name: /Manage/ }),
          `Enabled card at index ${i} should have a Manage link`
        ).toBeVisible();
      }
    }
  });
});
