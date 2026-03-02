export interface DataViewerPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
