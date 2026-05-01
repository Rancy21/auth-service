<script lang="ts">
  import { auth } from '$lib/auth.svelte.js';
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';

  onMount(() => {
    if (!auth.isAuthenticated && !auth.loading) {
      goto('/login');
    }
  });
</script>

<svelte:head>
  <title>Profile – Auth Service</title>
</svelte:head>

<div class="page">
  <h1>Your Profile</h1>

  {#if auth.isAuthenticated && auth.user}
    <dl>
      <dt>ID</dt>
      <dd>{auth.user.id}</dd>

      <dt>Email</dt>
      <dd>{auth.user.email} {auth.user.emailVerified ? '✅' : '⚠️'}</dd>

      <dt>Status</dt>
      <dd><span class="badge">{auth.user.status}</span></dd>

      <dt>Roles</dt>
      <dd>
        {#each auth.user.roles as role}
          <span class="badge badge-role">{role}</span>
        {/each}
      </dd>

      <dt>Created</dt>
      <dd>{new Date(auth.user.createdAt).toLocaleString()}</dd>

      <dt>Updated</dt>
      <dd>{new Date(auth.user.updatedAt).toLocaleString()}</dd>
    </dl>
  {:else if !auth.loading}
    <p class="redirect">
      You need to be logged in to view this page.
      <a href="/login">Go to login</a>
    </p>
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

  dl {
    display: grid;
    grid-template-columns: auto 1fr;
    gap: 0.75rem 1rem;
  }

  dt {
    font-weight: 600;
    color: #555;
    font-size: 0.85rem;
  }

  dd {
    margin: 0;
    word-break: break-all;
  }

  .badge {
    display: inline-block;
    padding: 0.15rem 0.5rem;
    border-radius: 4px;
    font-size: 0.8rem;
    background: #e8e8e8;
    color: #444;
  }

  .badge-role {
    background: #dbeafe;
    color: #1e40af;
    margin-right: 0.3rem;
  }

  .redirect {
    color: #888;
    text-align: center;
  }

  .redirect a { color: #1a1a2e; }
</style>
