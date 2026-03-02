import { registerPlugin } from '@capacitor/core';

import type { DataViewerPlugin } from './definitions';

const DataViewer = registerPlugin<DataViewerPlugin>('DataViewer', {
  web: () => import('./web').then((m) => new m.DataViewerWeb()),
});

export * from './definitions';
export { DataViewer };
