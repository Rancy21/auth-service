<script lang="ts">
  import { verifyEmail, resendVerification } from '$lib/api.js';
  import { goto } from '$app/navigation';
  import { browser } from '$app/environment';

  // Read ?token= from URL if present, otherwise show manual entry
  let token = $state('');
  let email = $state('');
  let message = $state<string | null>(null);
  let error = $state<string | null>(null);
  let submitting = $state(false);

  if (browser) {
    const params = new URLSearchParams(window.location.search);
    const tokenParam = params.get('token');
    if (tokenParam) {
      token = tokenParam;
      // Auto-submit if token is in URL
      handleVerify();
    }
  }

  async function handleVerify() {
    if (!token.trim()) return;
    error = null;
    message = null;
    submitting = true;

    try {
      const res = await verifyEmail(token.trim());
      message = res.message;
      setTimeout(() => goto('/login'), 3000);
    } catch (err: unknown) {
      error = (err as Error).message || 'Verification failed';
    } finally {
      submitting = false;
    }
  }

  async function handleResend() {
    if (!email.trim()) return;
    error = null;
    message = null;
    submitting = true;

    try {
      const res = await resendVerification(email.trim());
      message = res.message;
    } catch (err: unknown) {
      error = (err as Error).message || 'Failed to resend';
    } finally {
      submitting = false;
    }
  }
</script>

<svelte:head>
  <title>Verify Email – Auth Service</title>
</svelte:head>

<div class="page">
  <h1>Email Verification</h1>

  {#if error}
    <div class="alert alert-error">{error}</div>
  {/if}
  {#if message}
    <div class="alert alert-success">{message}</div>
  {/if}

  <div class="section">
    <label>
      Verification Token
      <textarea bind:value={token} rows="3" placeholder="Paste your verification token here…"></textarea>
    </label>
    <button onclick={handleVerify} disabled={submitting || !token.trim()}>
      {submitting ? 'Verifying…' : 'Verify Email'}
    </button>
  </div>

  <hr />

  <div class="section">
    <p class="hint">Didn't receive a token? Enter your email to resend:</p>
    <label>
      Email
      <input type="email" bind:value={email} placeholder="you@example.com" />
    </label>
    <button onclick={handleResend} disabled={submitting || !email.trim()}>
      Resend Verification
    </button>
  </div>
</div>

<style>
  .page {
    background: #fff;
    padding: 2rem;
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  }

  h1 { margin: 0 0 1.5rem; }

  .section {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  label { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.85rem; color: #555; }

  .hint { font-size: 0.85rem; color: #888; margin: 0; }

  input, textarea {
    padding: 0.6rem 0.75rem;
    border: 1px solid #ccc;
    border-radius: 6px;
    font-size: 0.95rem;
    font-family: monospace;
  }

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
  }

  button:disabled { opacity: 0.6; cursor: not-allowed; }

  hr { border: none; border-top: 1px solid #eee; margin: 1.5rem 0; }

  .alert { padding: 0.6rem 0.75rem; border-radius: 6px; font-size: 0.85rem; }
  .alert-error { background: #fff0f0; color: #c00; }
  .alert-success { background: #f0fff0; color: #060; }
</style>
