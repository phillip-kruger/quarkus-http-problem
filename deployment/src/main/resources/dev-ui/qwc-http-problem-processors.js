import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';

export class QwcHttpProblemProcessors extends LitElement {

    static styles = css`
        .processors-table {
            width: 100%;
            border-collapse: collapse;
        }
        .processors-table th, .processors-table td {
            padding: 8px 12px;
            text-align: left;
            border-bottom: 1px solid var(--lumo-contrast-10pct);
        }
        .processors-table th {
            color: var(--lumo-secondary-text-color);
            font-weight: 600;
        }
        .order-badge {
            display: inline-block;
            background: var(--lumo-primary-color-10pct);
            color: var(--lumo-primary-text-color);
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 0.85em;
        }
    `;

    static properties = {
        _processors: { state: true },
        _loading: { state: true }
    };

    constructor() {
        super();
        this._processors = [];
        this._loading = true;
        this.jsonRpc = new JsonRpc(this);
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getPostProcessors().then(response => {
            this._processors = response.result;
            this._loading = false;
        });
    }

    render() {
        if (this._loading) {
            return html`<p>Loading post-processors...</p>`;
        }

        if (this._processors.length === 0) {
            return html`<p>No post-processors registered.</p>`;
        }

        return html`
            <table class="processors-table">
                <thead>
                    <tr>
                        <th>Order</th>
                        <th>Processor</th>
                        <th>Class</th>
                        <th>Priority</th>
                    </tr>
                </thead>
                <tbody>
                    ${this._processors.map((p, i) => html`
                        <tr>
                            <td><span class="order-badge">${i + 1}</span></td>
                            <td>${p.name}</td>
                            <td><code>${p.className}</code></td>
                            <td>${p.priority}</td>
                        </tr>
                    `)}
                </tbody>
            </table>
        `;
    }
}

customElements.define('qwc-http-problem-processors', QwcHttpProblemProcessors);
