import { registerPlugin } from '@capacitor/core';
const DataViewer = registerPlugin('DataViewer', {
    web: () => import('./web').then((m) => new m.DataViewerWeb()),
});
export * from './definitions';
export { DataViewer };
//# sourceMappingURL=index.js.map