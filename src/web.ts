import { WebPlugin } from '@capacitor/core';

import type { DataViewerPlugin } from './definitions';

export class DataViewerWeb extends WebPlugin implements DataViewerPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
