import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AxiosMockAdapter from 'axios-mock-adapter';
import { gatewayClient, resetApiAuthState, tradeApi } from './api';
import { refreshToken, triggerReauthentication } from '../auth/keycloak';

vi.mock('../auth/keycloak', () => ({
  refreshToken: vi.fn(),
  triggerReauthentication: vi.fn(),
}));

describe('api auth session behavior', () => {
  let mock: AxiosMockAdapter;

  beforeEach(() => {
    mock = new AxiosMockAdapter(gatewayClient);
    resetApiAuthState();
  });

  afterEach(() => {
    mock.restore();
    vi.clearAllMocks();
  });

  it('uses current token at request time across user switches', async () => {
    vi.mocked(refreshToken)
      .mockResolvedValueOnce('token-user-a')
      .mockResolvedValueOnce('token-user-b');

    const seenAuthHeaders: string[] = [];
    mock.onGet('/api/v1/trades').reply((config) => {
      seenAuthHeaders.push(String(config.headers?.Authorization ?? ''));
      return [200, { success: true, data: [], message: 'ok', timestamp: new Date().toISOString() }];
    });

    await tradeApi.list();
    await tradeApi.list();

    expect(seenAuthHeaders).toEqual(['Bearer token-user-a', 'Bearer token-user-b']);
  });

  it('triggers a single deterministic reauth path on invalid session', async () => {
    vi.mocked(refreshToken).mockResolvedValue(undefined);
    const reauthPromise = Promise.resolve();
    vi.mocked(triggerReauthentication).mockReturnValue(reauthPromise);

    mock.onGet('/api/v1/trades').reply(401, {
      success: false,
      message: 'Unauthorized',
      data: null,
      timestamp: new Date().toISOString(),
    });

    await Promise.allSettled([tradeApi.list(), tradeApi.list()]);

    expect(triggerReauthentication).toHaveBeenCalledTimes(1);
  });

  it('retries once on 401 when a refreshed token becomes available', async () => {
    vi.mocked(refreshToken)
      .mockResolvedValueOnce(undefined)
      .mockResolvedValueOnce('token-after-refresh');
    vi.mocked(triggerReauthentication).mockResolvedValue();

    const seenAuthHeaders: string[] = [];
    mock.onGet('/api/v1/trades').replyOnce(401, {
      success: false,
      message: 'Unauthorized',
      data: null,
      timestamp: new Date().toISOString(),
    });
    mock.onGet('/api/v1/trades').reply((config) => {
      seenAuthHeaders.push(String(config.headers?.Authorization ?? ''));
      return [200, { success: true, data: [], message: 'ok', timestamp: new Date().toISOString() }];
    });

    const trades = await tradeApi.list();

    expect(trades).toEqual([]);
    expect(seenAuthHeaders).toContain('Bearer token-after-refresh');
    expect(triggerReauthentication).not.toHaveBeenCalled();
  });
});
