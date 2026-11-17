import { Observe } from 'expo-observe';
import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';

import { Button } from '@/components/Button';
import StorageInspector, { type StoredData } from '@/modules/storage-inspector';
import { useTheme } from '@/utils/theme';

// Shows the stored Observe data as text with test IDs, so the consent tests can read it on a
// simulator.
export function StoredDataSection() {
  const theme = useTheme();
  const [data, setData] = useState<StoredData | null>(() => StorageInspector?.inspect() ?? null);

  if (StorageInspector == null || data == null) {
    return null;
  }

  const rows: [string, string | number | boolean | null][] = [
    ['databaseExists', data.databaseExists],
    ['legacyStoreExists', data.legacyStoreExists],
    ['crashFileCount', data.crashFileCount],
    ['pendingErrorFileCount', data.pendingErrorFileCount],
    ['installationIdStored', data.installationIdStored],
    ['clientId', Observe.clientId],
  ];

  return (
    <>
      <Text style={[styles.sectionTitle, { color: theme.text.default }]}>Stored data</Text>
      {rows.map(([name, value]) => (
        <Text key={name} testID={`stored-data-${name}`} style={{ color: theme.text.default }}>
          {name}: {String(value)}
        </Text>
      ))}
      <Button
        title="Refresh stored data"
        onPress={() => setData(StorageInspector?.inspect() ?? null)}
        theme="secondary"
      />
    </>
  );
}

const styles = StyleSheet.create({
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 4,
  },
});
