import{j as r}from"./index-DtuzPm3T.js";function a(){return r.jsx("style",{children:`
      /* ── Card ── */
      .auth-card {
        background: #ffffff;
        border: 1px solid var(--border-1);
        border-radius: var(--radius-xl);
        padding: 2.4rem 2.4rem 2rem;
        width: 100%;
        opacity: 0;
        transform: translateY(10px);
        transition: opacity .3s ease, transform .3s ease;
        box-shadow: var(--shadow-md);
      }
      .auth-card--in { opacity: 1; transform: translateY(0); }

      /* ── Header ── */
      .auth-card-header { margin-bottom: 1.8rem; }

      .auth-card-eyebrow {
        display: inline-flex; align-items: center; gap: 6px;
        font-family: var(--font-mono);
        font-size: 0.62rem; font-weight: 700;
        letter-spacing: 0.13em; text-transform: uppercase;
        color: var(--accent); margin-bottom: 10px;
      }
      .auth-card-eyebrow-dot {
        width: 5px; height: 5px; border-radius: 50%;
        background: var(--accent); display: inline-block;
      }
      .auth-card-title {
        font-family: var(--font-display);
        font-size: 1.6rem; font-weight: 800;
        color: var(--text-0); letter-spacing: -0.02em;
        margin-bottom: 5px; line-height: 1.15;
      }
      .auth-card-sub {
        font-size: 0.82rem; color: var(--text-2); margin: 0;
      }

      /* ── Form ── */
      .auth-form  { display: flex; flex-direction: column; gap: 1.1rem; }
      .auth-field { display: flex; flex-direction: column; gap: 6px; }
      .auth-label {
        font-size: 0.74rem; font-weight: 700;
        letter-spacing: 0.04em; color: var(--text-1);
      }

      .auth-input-wrap { position: relative; }
      .auth-input {
        width: 100%; padding: 11px 14px;
        background: var(--bg-2);
        border: 1.5px solid var(--border-1);
        border-radius: var(--radius-md);
        font-family: var(--font-mono); font-size: 0.85rem;
        color: var(--text-0); outline: none;
        transition: border-color .15s, box-shadow .15s, background .15s;
      }
      .auth-input::placeholder { color: var(--text-3); }
      .auth-input:hover:not(:focus) {
        border-color: rgba(37,99,235,0.3);
        background: #ffffff;
      }
      .auth-input:focus {
        border-color: var(--accent);
        background: #ffffff;
        box-shadow: 0 0 0 3px var(--accent-glow);
      }
      .auth-input--padded { padding-right: 44px; }
      .auth-input--err    { border-color: var(--red) !important; background: var(--red-light) !important; }
      .auth-input--err:focus { box-shadow: 0 0 0 3px rgba(220,38,38,.10) !important; }

      .auth-eye {
        position: absolute; right: 12px; top: 50%; transform: translateY(-50%);
        background: none; border: none; color: var(--text-3);
        cursor: pointer; font-size: 0.85rem; padding: 2px;
        line-height: 1; transition: color .15s;
      }
      .auth-eye:hover { color: var(--accent); }

      .auth-err    { font-size: 0.72rem; color: var(--red); font-weight: 500; }
      .auth-helper { font-size: 0.72rem; color: var(--text-2); }

      /* ── Password strength ── */
      .pw-strength { display: flex; flex-direction: column; gap: 4px; }
      .pw-bar  { height: 3px; border-radius: 2px; background: var(--bg-3); overflow: hidden; }
      .pw-fill { height: 100%; border-radius: 2px; transition: width .3s, background .3s; }
      .pw-label{ font-size: 0.67rem; font-weight: 700; letter-spacing: 0.06em; text-transform: uppercase; }

      /* ── Submit button ── */
      .auth-btn {
        margin-top: 6px; width: 100%; padding: 12px;
        background: var(--accent); color: #fff;
        border: none; border-radius: var(--radius-md);
        font-family: var(--font-display); font-size: 0.92rem; font-weight: 700;
        cursor: pointer; letter-spacing: .01em;
        transition: background .15s, box-shadow .15s, transform .1s;
      }
      .auth-btn:hover:not(:disabled) {
        background: var(--accent-dim);
        box-shadow: var(--shadow-accent);
      }
      .auth-btn:active:not(:disabled) { transform: scale(.99); }
      .auth-btn:disabled { opacity: .5; cursor: not-allowed; }
      .auth-btn-spin {
        display: inline-block; width: 14px; height: 14px;
        border: 2px solid rgba(255,255,255,0.3);
        border-top-color: #fff; border-radius: 50%;
        animation: spin .6s linear infinite; vertical-align: middle;
      }

      /* ── Divider ── */
      .auth-divider { display: flex; align-items: center; gap: 10px; margin: 1.3rem 0 .8rem; }
      .auth-divider-line { flex: 1; height: 1px; background: var(--border-1); }
      .auth-divider-text { font-size: 0.68rem; color: var(--text-3); letter-spacing: .08em; text-transform: uppercase; }

      /* ── Switch ── */
      .auth-switch { text-align: center; font-size: 0.83rem; color: var(--text-2); margin: 0; }
      .auth-link   { color: var(--accent); font-weight: 700; transition: color .15s; }
      .auth-link:hover { color: var(--accent-dim); text-decoration: underline; }

      /* ── Banners ── */
      .auth-banner { border-radius: var(--radius-md); padding: .9rem 1.1rem; margin-bottom: 1rem; font-size: .82rem; line-height: 1.5; }
      .auth-banner--success { background: var(--green-light);  border: 1px solid rgba(22,163,74,.2); color: var(--green); }
      .auth-banner--error   { background: var(--red-light);   border: 1px solid rgba(220,38,38,.2); color: var(--red); }
      .auth-banner--info    { background: var(--accent-light); border: 1px solid var(--border-0);   color: var(--accent); }
      .auth-banner-icon { font-size: 1.1rem; margin-bottom: 4px; display: block; }
    `})}export{a as A};
