import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';

export class QwcHttpProblemTest extends LitElement {

    static styles = css`
        .test-form {
            display: flex;
            gap: 12px;
            align-items: flex-end;
            margin-bottom: 16px;
        }
        .field {
            display: flex;
            flex-direction: column;
            gap: 4px;
        }
        .field label {
            font-size: 0.85em;
            color: var(--lumo-secondary-text-color);
        }
        .field input {
            padding: 6px 10px;
            border: 1px solid var(--lumo-contrast-20pct);
            border-radius: 4px;
            background: var(--lumo-base-color);
            color: var(--lumo-body-text-color);
        }
        .field input:focus {
            outline: 2px solid var(--lumo-primary-color);
            border-color: transparent;
        }
        button {
            padding: 6px 16px;
            background: var(--lumo-primary-color);
            color: var(--lumo-primary-contrast-color);
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }
        button:hover {
            opacity: 0.9;
        }
        button:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }
        .result {
            background: var(--lumo-contrast-5pct);
            border-radius: 6px;
            padding: 16px;
        }
        .result pre {
            margin: 0;
            white-space: pre-wrap;
            word-break: break-word;
            color: var(--lumo-body-text-color);
            font-family: monospace;
        }
        .result-label {
            font-size: 0.85em;
            color: var(--lumo-secondary-text-color);
            margin-bottom: 8px;
        }
    `;

    static properties = {
        _statusCode: { state: true },
        _detail: { state: true },
        _result: { state: true },
        _loading: { state: true }
    };

    constructor() {
        super();
        this._statusCode = 500;
        this._detail = '';
        this._result = null;
        this._loading = false;
        this.jsonRpc = new JsonRpc(this);
    }

    _test() {
        this._loading = true;
        this.jsonRpc.testProblem({
            statusCode: parseInt(this._statusCode),
            detail: this._detail || null
        }).then(response => {
            this._result = response.result;
            this._loading = false;
        });
    }

    render() {
        return html`
            <div class="test-form">
                <div class="field">
                    <label>Status Code</label>
                    <input type="number" min="100" max="599"
                        .value="${this._statusCode}"
                        @input="${e => this._statusCode = e.target.value}">
                </div>
                <div class="field">
                    <label>Detail (optional)</label>
                    <input type="text" placeholder="e.g. Resource not found"
                        .value="${this._detail}"
                        @input="${e => this._detail = e.target.value}">
                </div>
                <button @click="${this._test}" ?disabled="${this._loading}">
                    ${this._loading ? 'Testing...' : 'Test'}
                </button>
            </div>

            ${this._result ? html`
                <div class="result">
                    <div class="result-label">Response (application/problem+json)</div>
                    <pre>${JSON.stringify(this._result, null, 2)}</pre>
                </div>
            ` : ''}
        `;
    }
}

customElements.define('qwc-http-problem-test', QwcHttpProblemTest);
