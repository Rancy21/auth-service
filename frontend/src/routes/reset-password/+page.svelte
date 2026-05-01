<script lang="ts">
  import { requestPasswordReset, confirmPasswordReset } from '$lib/api.js';
  import { goto } from '$app/navigation';
  import { browser } from '$app/environment';

  // Read ?token= from URL – if present, show confirm form; otherwise, show request form
  let mode = $state<'request' | 'confirm'>('request');

  // Request form
  let email = $state('');

  // Confirm form
  let token = $state('');
  let password = $state('');
  let confirmPass = $state('');

  let message = $state<string | null>(null);
  let error = $state<string | null>(null);
  let submitting = $state(false);

  if (browser) {
    const params = new URLSearchParams(window.location.search);
    const tokenParam = params.get('token');
    if (tokenParam) {
      token = tokenParam;
      mode = 'confirm';
    }
  }

  async function handleRequest() {
    error = null;
    message = null;
    submitting = true;

    try {
      const res = await requestPasswordReset({ email });
      message = res.message;
    } catch (err: unknown) {
      error = (err as Error).message || 'Request failed';
    } finally {
      submitting = false;
    }
  }

  async function handleConfirm() {
    error = null;
    message = null;

    if (password !== confirmPass) {
      error = 'Passwords do not match';
      return;
    }

    submitting = true;
    try {
      const res = await confirmPasswordReset({ token, password });
      message = res.message;
      setTimeout(() => goto('/login'), 3000);
    } catch (err: unknown) {
      error = (err as Error).message || 'Reset failed';
    } finally {
      submitting = false;
    }
  }
</script>

<svelte:head>
  <title>Reset Password – Auth Service</title>
</svelte:head>

<div class="page">
  <h1>Reset Password</h1>

  {#if error}
    <div class="alert alert-error">{error}</div>
  {/if}
  {#if message}
    <div class="alert alert-success">{message}</div>
  {/if}

  {#if mode === 'request'}
    <form onsubmit={(e) => { e.preventDefault(); handleRequest(); }}>
      <label>
        Email
        <input
          type="email"
          bind:value={email}
          required
          placeholder="you@example.com"
        />
      </label>
      <button type="submit" disabled={submitting}>
        {submitting ? 'Sending…' : 'Send Reset Link'}
      </button>
    </form>

    <p class="footer-links">
      <a href="/login">← Back to login</a>
    </p>
  {:else}
    <form onsubmit={(e) => { e.preventDefault(); handleConfirm(); }}>
      <label>
        Reset Token
        <textarea bind:value={token} rows="3" placeholder="Paste your reset token…"></textarea>
      </label>

      <label>
        New Password
        <input
          type="password"
          bind:value={password}
          required
          minlength="8"
          autocomplete="new-password"
        />
        <span class="hint">Minimum 8 characters</span>
      </label>

      <label>
        Confirm New Password
        <input
          type="password"
          bind:value={confirmPass}
          required
          minlength="8"
          autocomplete="new-password"
        />
      </label>

      <button type="submit" disabled={submitting}>
        {submitting ? 'Resetting…' : 'Reset Password'}
      </button>
    </form>
  {/if}
</div>

<style>
  .page {
    background: #fff;
    padding: 2rem;
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  }

  h1 { margin: 0 0 1.5rem; }

  form { display: flex; flex-direction: column; gap: 1rem; }

  label { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.85rem; color: #555; }

  .hint { font-size: 0.75rem; color: #999; }

  input, textarea {
    padding: 0.6rem 0.75rem;
    border: 1px solid #ccc;
    border-radius: 6px;
    font-size: 0.95rem;
  }

  textarea { font-family: monospace; resize: vertical; }

  input:focus, textarea:focus { border-color: #1a1a2e; outline: none; }

  button {
    padding: 0.65rem;
    background: #1a1a2e;
    color: #fff;
    border: none;
    border-radius: 6px;
    font-size: 0.95rem;
    cursor: pointer;
    font-weight: 500;
    margin-top: 0.5rem;
  }

  button:disabled { opacity: 0.6; cursor: not-allowed; }

  .alert { padding: 0.6rem 0.75rem; border-radius: 6px; font-size: 0.85rem; }
  .alert-error { background: #fff0f0; color: #c00; }
  .alert-success { background: #f0fff0; color: #060; }

  .footer-links { text-align: center; margin-top: 1.5rem; font-size: 0.85rem; }
  .footer-links a { color: #1a1a2e; }
</style>
