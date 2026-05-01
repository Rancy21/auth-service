<script lang="ts">
  import { auth } from '$lib/auth.svelte.js';
</script>

<svelte:head>
  <title>Auth Service – Home</title>
</svelte:head>

<div class="hero">
  <h1>Full-Stack Auth Service</h1>
  <p>A production-grade authentication system with JWT, refresh tokens, OAuth, email verification, password reset, and role-based access.</p>

  {#if auth.isAuthenticated}
    <div class="card">
      <p>✅ You are logged in as <strong>{auth.user?.email}</strong></p>
      <p>Roles: {auth.user?.roles.join(', ') || 'none'}</p>
      <a href="/profile" class="btn">View Profile</a>
    </div>
  {:else}
    <div class="actions">
      <a href="/login" class="btn btn-primary">Login</a>
      <a href="/register" class="btn">Register</a>
    </div>
  {/if}
</div>

<section class="features">
  <div class="feature">
    <h3>📧 Email/Password Auth</h3>
    <p>Secure registration and login with bcrypt-hashed passwords.</p>
  </div>
  <div class="feature">
    <h3>🔐 JWT + Refresh Tokens</h3>
    <p>Short-lived access tokens with httpOnly cookie-based refresh token rotation.</p>
  </div>
  <div class="feature">
    <h3>🔗 OAuth 2.0</h3>
    <p>Login with Google or GitHub via OAuth 2.0 flows.</p>
  </div>
  <div class="feature">
    <h3>📬 Email Verification</h3>
    <p>Async email verification with token-based confirmation links.</p>
  </div>
  <div class="feature">
    <h3>🔄 Password Reset</h3>
    <p>Secure password reset flow with time-limited reset tokens.</p>
  </div>
  <div class="feature">
    <h3>🛡️ Role-Based Access</h3>
    <p>USER and ADMIN roles with method-level authorization.</p>
  </div>
</section>

<style>
  .hero {
    text-align: center;
    padding: 2rem 0;
  }

  .hero h1 {
    font-size: 2rem;
    margin-bottom: 0.5rem;
  }

  .hero p {
    color: #555;
    max-width: 440px;
    margin: 0 auto 1.5rem;
    line-height: 1.5;
  }

  .card {
    background: #fff;
    border-radius: 8px;
    padding: 1.5rem;
    box-shadow: 0 1px 4px rgba(0,0,0,0.08);
    text-align: center;
  }

  .card p {
    margin: 0.5rem 0;
  }

  .actions {
    display: flex;
    gap: 1rem;
    justify-content: center;
  }

  .btn {
    display: inline-block;
    padding: 0.6rem 1.5rem;
    border-radius: 6px;
    text-decoration: none;
    font-weight: 500;
    border: 1px solid #ccc;
    background: #fff;
    color: #333;
    cursor: pointer;
  }

  .btn-primary {
    background: #1a1a2e;
    color: #fff;
    border-color: #1a1a2e;
  }

  .btn:hover {
    opacity: 0.85;
  }

  .features {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 1.25rem;
    margin-top: 2rem;
  }

  .feature {
    background: #fff;
    padding: 1.25rem;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  }

  .feature h3 {
    margin: 0 0 0.5rem;
    font-size: 1rem;
  }

  .feature p {
    margin: 0;
    font-size: 0.85rem;
    color: #666;
    line-height: 1.4;
  }
</style>
