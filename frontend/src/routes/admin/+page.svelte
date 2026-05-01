<script lang="ts">
  import { auth } from '$lib/auth.svelte.js';
  import { listUsers } from '$lib/api.js';
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';
  import type { UserSummary } from '$lib/types.js';

  let users = $state<UserSummary[]>([]);
  let error = $state<string | null>(null);
  let loading = $state(true);

  async function fetchUsers() {
    loading = true;
    error = null;
    try {
      users = await listUsers();
    } catch (err: unknown) {
      error = (err as Error).message || 'Failed to fetch users';
      if ((err as { status?: number }).status === 403) {
        error = 'Access denied – Admin role required.';
      }
    } finally {
      loading = false;
    }
  }

  onMount(() => {
    if (!auth.isAuthenticated && !auth.loading) {
      goto('/login');
      return;
    }
    if (auth.isAdmin) {
      fetchUsers();
    }
  });
</script>

<svelte:head>
  <title>Admin Dashboard – Auth Service</title>
</svelte:head>

<div class="page">
  <h1>Admin Dashboard</h1>

  {#if !auth.isAdmin}
    <p class="redirect">
      You do not have access to this page (ADMIN role required).
      <a href="/">Go home</a>
    </p>
  {:else if loading}
    <p class="loading">Loading users…</p>
  {:else if error}
    <div class="alert alert-error">{error}</div>
    <button onclick={fetchUsers} class="retry">Retry</button>
  {:else}
    <table>
      <thead>
        <tr>
          <th>Email</th>
          <th>Verified</th>
          <th>Status</th>
          <th>Roles</th>
          <th>Created</th>
        </tr>
      </thead>
      <tbody>
        {#each users as u}
          <tr>
            <td>{u.email}</td>
            <td>{u.emailVerified ? '✅' : '❌'}</td>
            <td><span class="badge">{u.status}</span></td>
            <td>
              {#each u.roles as role}
                <span class="badge badge-role">{role}</span>
              {/each}
            </td>
            <td>{new Date(u.createdAt).toLocaleDateString()}</td>
          </tr>
        {/each}
      </tbody>
    </table>
  {/if}
</div>

<style>
  .page {
    background: #fff;
    padding: 2rem;
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.08);
    max-width: 780px !important;
  }

  h1 { margin: 0 0 1.5rem; }

  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.85rem;
  }

  th {
    text-align: left;
    padding: 0.6rem 0.75rem;
    background: #f9f9f9;
    border-bottom: 2px solid #ddd;
    font-weight: 600;
  }

  td {
    padding: 0.6rem 0.75rem;
    border-bottom: 1px solid #eee;
  }

  tr:hover td { background: #fafafa; }

  .badge {
    display: inline-block;
    padding: 0.1rem 0.4rem;
    border-radius: 4px;
    font-size: 0.75rem;
    background: #e8e8e8;
  }

  .badge-role {
    background: #dbeafe;
    color: #1e40af;
    margin-right: 0.2rem;
  }

  .alert { padding: 0.6rem 0.75rem; border-radius: 6px; font-size: 0.85rem; }
  .alert-error { background: #fff0f0; color: #c00; }

  .retry {
    margin-top: 0.75rem;
    padding: 0.5rem 1rem;
    border: 1px solid #ccc;
    border-radius: 6px;
    background: #fff;
    cursor: pointer;
  }

  .loading, .redirect { text-align: center; color: #888; }
  .redirect a { color: #1a1a2e; }
</style>
