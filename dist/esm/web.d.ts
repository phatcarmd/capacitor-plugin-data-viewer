import { WebPlugin } from '@capacitor/core';
import type { DataViewerPlugin } from './definitions';
export declare class DataViewerWeb extends WebPlugin implements DataViewerPlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    explore(): Promise<void>;
}
