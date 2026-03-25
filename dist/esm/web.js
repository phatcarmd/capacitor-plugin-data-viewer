import { WebPlugin } from '@capacitor/core';
export class DataViewerWeb extends WebPlugin {
    async echo(options) {
        console.log('ECHO', options);
        return options;
    }
    async explore() { }
    async startNetworkTracking() { }
}
//# sourceMappingURL=web.js.map