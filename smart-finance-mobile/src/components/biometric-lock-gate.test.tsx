jest.mock('@/lib/biometric-lock', () => ({
  isBiometricLockEnabled: jest.fn(),
  isBiometricAvailable: jest.fn(),
  authenticate: jest.fn(),
}));

import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import { Text } from 'react-native';

import { authenticate, isBiometricAvailable, isBiometricLockEnabled } from '@/lib/biometric-lock';

import { BiometricLockGate } from './biometric-lock-gate';

const mockedIsBiometricLockEnabled = isBiometricLockEnabled as jest.MockedFunction<typeof isBiometricLockEnabled>;
const mockedIsBiometricAvailable = isBiometricAvailable as jest.MockedFunction<typeof isBiometricAvailable>;
const mockedAuthenticate = authenticate as jest.MockedFunction<typeof authenticate>;

describe('BiometricLockGate', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders nothing while it is still resolving whether the lock applies', async () => {
    mockedIsBiometricLockEnabled.mockReturnValue(new Promise(() => {}));
    mockedIsBiometricAvailable.mockReturnValue(new Promise(() => {}));

    await render(
      <BiometricLockGate>
        <Text>Contenido protegido</Text>
      </BiometricLockGate>,
    );

    expect(screen.queryByText('Contenido protegido')).toBeNull();
    expect(screen.queryByText('KoroFin está bloqueado')).toBeNull();
  });

  it('renders the children directly when the lock is disabled', async () => {
    mockedIsBiometricLockEnabled.mockResolvedValue(false);
    mockedIsBiometricAvailable.mockResolvedValue(true);

    await render(
      <BiometricLockGate>
        <Text>Contenido protegido</Text>
      </BiometricLockGate>,
    );

    await waitFor(() => expect(screen.getByText('Contenido protegido')).toBeTruthy());
  });

  it('shows the lock screen and unlocks after a successful biometric prompt', async () => {
    mockedIsBiometricLockEnabled.mockResolvedValue(true);
    mockedIsBiometricAvailable.mockResolvedValue(true);
    mockedAuthenticate.mockResolvedValue(true);

    await render(
      <BiometricLockGate>
        <Text>Contenido protegido</Text>
      </BiometricLockGate>,
    );

    await waitFor(() => expect(screen.getByText('KoroFin está bloqueado')).toBeTruthy());
    expect(screen.queryByText('Contenido protegido')).toBeNull();

    fireEvent.press(screen.getByText('Desbloquear'));

    await waitFor(() => expect(screen.getByText('Contenido protegido')).toBeTruthy());
  });

  it('stays locked when the biometric prompt fails or is cancelled', async () => {
    mockedIsBiometricLockEnabled.mockResolvedValue(true);
    mockedIsBiometricAvailable.mockResolvedValue(true);
    mockedAuthenticate.mockResolvedValue(false);

    await render(
      <BiometricLockGate>
        <Text>Contenido protegido</Text>
      </BiometricLockGate>,
    );

    await waitFor(() => expect(screen.getByText('KoroFin está bloqueado')).toBeTruthy());
    fireEvent.press(screen.getByText('Desbloquear'));

    await waitFor(() => expect(mockedAuthenticate).toHaveBeenCalledTimes(1));
    expect(screen.queryByText('Contenido protegido')).toBeNull();
  });
});
