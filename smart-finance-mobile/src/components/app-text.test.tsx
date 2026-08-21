import { render, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';

import { AppText } from './app-text';

describe('AppText', () => {
  it('renderiza su contenido de texto', async () => {
    await render(<AppText>Hola</AppText>);

    expect(screen.getByText('Hola')).toBeTruthy();
  });

  it('mergea un style explicito con la fuente global (Inter)', async () => {
    await render(<AppText style={{ color: 'red' }}>Hola</AppText>);

    const node = screen.getByText('Hola');
    const flattenedStyle = StyleSheet.flatten(node.props.style);

    expect(flattenedStyle).toMatchObject({ fontFamily: 'Inter', color: 'red' });
  });
});
