'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var core = require('@capacitor/core');

const DataViewer = core.registerPlugin('DataViewer', {
    web: () => Promise.resolve().then(function () { return web; }).then((m) => new m.DataViewerWeb()),
});

class DataViewerWeb extends core.WebPlugin {
    async echo(options) {
        console.log('ECHO', options);
        return options;
    }
    async explore() {
    }
}

var web = /*#__PURE__*/Object.freeze({
    __proto__: null,
    DataViewerWeb: DataViewerWeb
});

exports.DataViewer = DataViewer;
//# sourceMappingURL=plugin.cjs.js.map
