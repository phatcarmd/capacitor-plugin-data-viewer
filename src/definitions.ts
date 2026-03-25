export interface DataViewerPlugin {
  explore(): Promise<void>;
  startNetworkTracking(): Promise<void>;
  stopNetworkTracking(): Promise<void>;
}
