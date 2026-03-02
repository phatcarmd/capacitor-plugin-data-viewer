import { DataViewer } from 'capacitor-plugin-data-viewer';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    DataViewer.echo({ value: inputValue })
}
