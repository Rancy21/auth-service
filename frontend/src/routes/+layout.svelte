<script lang="ts">
  import favicon from '$lib/assets/favicon.svg';
  import { auth } from '$lib/auth.svelte.js';
  import { onMount } from 'svelte';

  let { children } = $props();

  onMount(() => {
    auth.init();
  });
</script>

<svelte:head>
  <link rel="icon" href={favicon} />
</svelte:head>

<nav class="navbar">
  <a href="/" class="logo">🔐 Auth Service</a>

  <div class="nav-links">
    {#if auth.isAuthenticated}
      <a href="/profile">{auth.user?.email}</a>
      {#if auth.isAdmin}
        <a href="/admin">Admin</a>
      {/if}
      <button class="btn-link" onclick={() => auth.logout()}>Logout</button>
    {:else}
      <a href="/login">Login</a>
      <a href="/register">Register</a>
    {/if}
  </div>
</nav>

<main>
  {#if auth.loading}
    <div class="spinner">Loading…</div>
  {:else}
    {@render children()}
  {/if}
</main>

<style>
  :global(*) {
    box-sizing: border-box;
  }

  :global(body) {
    margin: 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
      Oxygen, Ubuntu, sans-serif;
    background: #f5f5f5;
    color: #222;
  }

  .navbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 2rem;
    height: 56px;
    background: #1a1a2e;
    color: #eee;
  }

  .navbar .logo {
    color: #00d4ff;
    font-weight: 700;
    font-size: 1.15rem;
    text-decoration: none;
  }

  .nav-links {
    display: flex;
    gap: 1.25rem;
    align-items: center;
  }

  .nav-links a {
    color: #ccc;
    text-decoration: none;
    font-size: 0.9rem;
  }

  .nav-links a:hover {
    color: #fff;
  }

  .btn-link {
    background: none;
    border: none;
    color: #ff6b6b;
    cursor: pointer;
    font-size: 0.9rem;
  }

  .btn-link:hover {
    color: #ff4444;
  }

  main {
    max-width: 520px;
    margin: 2.5rem auto;
    padding: 0 1rem;
  }

  .spinner {
    text-align: center;
    padding: 3rem;
    color: #888;
  }
</style>
