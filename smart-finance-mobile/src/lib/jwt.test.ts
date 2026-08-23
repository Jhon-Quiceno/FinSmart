import { decodeJwtPayload } from './jwt';

describe('decodeJwtPayload', () => {
  it('decodes a real base64url-encoded JWT payload', () => {
    // header.payload.signature donde payload = {"sub":"42","email":"x@y.com"}
    const token = 'header.eyJzdWIiOiI0MiIsImVtYWlsIjoieEB5LmNvbSJ9.signature';

    const payload = decodeJwtPayload<{ sub: string; email: string }>(token);

    expect(payload).toEqual({ sub: '42', email: 'x@y.com' });
  });

  it('returns null for a malformed token', () => {
    expect(decodeJwtPayload('not-a-jwt')).toBeNull();
  });

  it('returns null when the payload segment is not valid JSON', () => {
    expect(decodeJwtPayload('header.not-base64-json.signature')).toBeNull();
  });
});
