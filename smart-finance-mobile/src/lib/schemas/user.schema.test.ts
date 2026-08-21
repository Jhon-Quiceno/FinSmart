import { changePasswordSchema } from './user.schema';

describe('changePasswordSchema', () => {
  it('acepta cuando newPassword y confirmPassword coinciden', () => {
    const result = changePasswordSchema.safeParse({
      currentPassword: 'old-pass',
      newPassword: 'new-pass-123',
      confirmPassword: 'new-pass-123',
    });

    expect(result.success).toBe(true);
  });

  it('rechaza cuando newPassword y confirmPassword no coinciden, con el error en confirmPassword', () => {
    const result = changePasswordSchema.safeParse({
      currentPassword: 'old-pass',
      newPassword: 'new-pass-123',
      confirmPassword: 'otra-cosa',
    });

    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].path).toEqual(['confirmPassword']);
      expect(result.error.issues[0].message).toBe('Las contrasenas no coinciden');
    }
  });
});
