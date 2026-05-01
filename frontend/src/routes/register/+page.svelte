<script lang="ts">
  import { auth } from '$lib/auth.svelte.js';
  import { goto } from '$app/navigation';

  let email = $state('');
  let password = $state('');
  let confirm = $state('');
  let error = $state<string | null>(null);
  let success = $state<string | null>(null);
  let submitting = $state(false);

  async function handleRegister(e: SubmitEvent) {
    e.preventDefault();
    error = null;
    success = null;

    if (password !== confirm) {
      error = 'Passwords do not match';
      return;
    }

    submitting = true;
    try {
      await auth.register(email, password);
      success = 'Registration successful! Check your email to verify your account.';
      email = '';
      password = '';
      confirm = '';
    } catch (err: unknown) {
      error = (err as Error).message || 'Registration failed';
    } finally {
      submitting = false;
    }
  }
</script>

<svelte:head>
  <title>Register – Auth Service</title>
</svelte:head>

<div class="page">
  <h1>Create Account</h1>

  <form onsubmit={handleRegister}>
    {#if error}
      <div class="alert alert-error">{error}</div>
    {/if}
    {#if success}
      <div class="alert alert-success">{success}</div>
    {/if}

    <label>
      Email
      <input
        type="email"
        bind:value={email}
        required
        autocomplete="email"
      />
    </label>

    <label>
      Password
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
      Confirm Password
      <input
        type="password"
        bind:value={confirm}
        required
        minlength="8"
        autocomplete="new-password"
      />
    </label>

    <button type="submit" disabled={submitting}>
      {submitting ? 'Creating account…' : 'Register'}
    </button>
  </form>

  <p class="footer-links">
    Already have an account? <a href="/login">Login</a>
  </p>
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

  input {
    padding: 0.6rem 0.75rem;
    border: 1px solid #ccc;
    border-radius: 6px;
    font-size: 0.95rem;
  }

  input:focus { border-color: #1a1a2e; outline: none; }

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
