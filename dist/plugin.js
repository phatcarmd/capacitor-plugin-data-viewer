var capacitorDataViewer = (function (exports, core) {
    'use strict';

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

    Object.defineProperty(exports, '__esModule', { value: true });

    return exports;

})({}, capacitorExports);
//# sourceMappingURL=plugin.js.map
