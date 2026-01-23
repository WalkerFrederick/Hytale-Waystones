# Waystones Plugin - Permissions Guide

This document provides a comprehensive overview of all permissions available in the Waystones plugin.

## Permission Defaults Overview

| Permission | Description | OP | Player |
|------------|-------------|:--:|:------:|
| `hytale.command.waystones.allowListMenu` | Access the `/waystones list` command | ✅ | ❌ |
| `hytale.command.waystones.allowEditAll` | Edit any waystone's settings | ✅ | ❌ |
| `hytale.command.waystones.allowSeeAllPrivate` | View and teleport to all private waystones | ✅ | ❌ |
| `hytale.command.waystones.allowPrivateWaystoneRemoval` | Break private waystones owned by others | ✅ | ❌ |
| `hytale.command.waystones.blockWaystonePlacement` | Prevent placing waystone blocks | ❌* | ❌ |
| `hytale.command.waystones.blockWaystoneRemoval` | Prevent breaking waystone blocks | ❌* | ❌ |
| `hytale.command.waystones.blockPublicWaystoneCreation` | Force all new waystones to be private | ❌* | ❌ |
| `hytale.command.waystones.maxWaystones.X` | Limit player to X waystones (e.g., maxWaystones.5) | ❌* | ❌ |

> **Note:** Permissions marked with ❌* are "deny" permissions or limits. OPs bypass these restrictions entirely.

---

## Detailed Permission Descriptions

### Allow Permissions

These permissions grant additional capabilities to users.

---

#### `hytale.command.waystones.allowListMenu`

**Purpose:** Grants access to the `/waystones list` command.

**Behavior:**
- Without this permission, players cannot use the `/waystones list` command
- Players can still access waystones by right-clicking on waystone blocks
- This permission is primarily for administrative access to the waystone list without needing to be at a waystone

**Use Case:** Give to moderators or staff who need quick access to the waystone network.

---

#### `hytale.command.waystones.allowEditAll`

**Purpose:** Allows editing any waystone's settings, regardless of ownership.

**Behavior:**
- Grants access to the edit button (pencil icon) on waystone entries in the list
- Enables access to OP-only settings like Priority and Server Owned toggles
- Without this permission, players can only edit waystones they own

**What can be edited:**
- Waystone name
- Visibility (public/private)
- Teleport direction (north/south/east/west)
- Player orientation (facing towards/away from statue)
- Priority (sort order in the list)
- Server Owned status

**Use Case:** Administrators who need to manage the waystone network.

---

#### `hytale.command.waystones.allowSeeAllPrivate`

**Purpose:** View and teleport to all private waystones, even those owned by other players.

**Behavior:**
- Private waystones from all players appear in the waystone list
- Can teleport to any visible waystone (if you can see it, you can use it)
- Useful for monitoring the waystone network

**Use Case:** Staff who need to oversee all waystones for moderation purposes.

---

#### `hytale.command.waystones.allowPrivateWaystoneRemoval`

**Purpose:** Allows breaking private waystone blocks owned by other players.

**Behavior:**
- By default, players cannot break private waystones they don't own
- This permission bypasses that restriction
- Does NOT allow breaking server-owned waystones (only OPs can do that)

**Use Case:** Moderators who need to remove abandoned or problematic private waystones.

---

### Block Permissions (Deny List)

These permissions restrict capabilities. They act as a "deny list" - if a user has the permission, the action is blocked. **OPs are immune to all block permissions.**

---

#### `hytale.command.waystones.blockWaystonePlacement`

**Purpose:** Prevents a user from placing waystone blocks in the world.

**Behavior:**
- When a player with this permission tries to place a waystone block, the placement is cancelled
- The block is not consumed from their inventory
- OPs bypass this restriction entirely

**Use Case:** Restrict certain player groups from creating new waystones while still allowing them to use existing ones.

**Example:**
```
/perm user add <uuid> hytale.command.waystones.blockWaystonePlacement
```

---

#### `hytale.command.waystones.blockWaystoneRemoval`

**Purpose:** Prevents a user from breaking any waystone blocks.

**Behavior:**
- When a player with this permission tries to break a waystone, the break is cancelled
- Applies to all waystones (public, private, and server-owned)
- OPs bypass this restriction entirely

**Use Case:** Protect the waystone network by preventing certain users from destroying waystones.

**Example:**
```
/perm user add <uuid> hytale.command.waystones.blockWaystoneRemoval
```

---

#### `hytale.command.waystones.blockPublicWaystoneCreation`

**Purpose:** Forces all new waystones created by the user to be private.

**Behavior:**
- New waystones default to **private** instead of public
- The visibility dropdown is **hidden** in the settings page
- User cannot change an existing waystone's visibility to public
- OPs bypass this restriction (their waystones default to public, and they see the visibility dropdown)

**Use Case:** Limit waystone spam on public servers - players can create personal waystones but cannot clutter the public network.

**Example:**
```
/perm user add <uuid> hytale.command.waystones.blockPublicWaystoneCreation
```

---

#### `hytale.command.waystones.maxWaystones.X`

**Purpose:** Limits the maximum number of waystones a player can own.

**Behavior:**
- Replace `X` with a number (e.g., `maxWaystones.1`, `maxWaystones.5`, `maxWaystones.10`)
- If a player has multiple `maxWaystones.X` permissions, the **highest** value is used
- When a player tries to place a new waystone and they've reached their limit, they receive the message: *"You have reached your maximum waystone limit (X)"*
- If no `maxWaystones.X` permission is set, the player has **unlimited** waystones
- OPs bypass this restriction entirely (unlimited waystones)

**Use Case:** Control resource usage on servers by limiting how many waystones each player can create.

**Examples:**
```bash
# Allow player to have max 3 waystones
/perm user add <uuid> hytale.command.waystones.maxWaystones.3

# Allow VIP group to have max 10 waystones
/perm group add VIP hytale.command.waystones.maxWaystones.10

# Allow all players to have max 5 waystones (via default group)
/perm group add default hytale.command.waystones.maxWaystones.5
```

**Stacking Example:**
If a player has both `maxWaystones.3` (from default group) and `maxWaystones.10` (from VIP group), they can create **10** waystones (the highest value wins).

---

## Protection Rules

Beyond permissions, the waystone system has built-in protection rules:

### Server-Owned Waystones
- Only **OPs** can break server-owned waystones
- Non-OPs receive the message: *"You do not have permission to break this Waystone"*
- Server-owned waystones don't display an owner name in the UI

### Private Waystones
- By default, only the **owner** can break their private waystone
- Players with `allowPrivateWaystoneRemoval` permission can break others' private waystones
- OPs can always break any waystone

### Public Waystones
- Anyone can break a public waystone they own
- OPs can break any public waystone
- Breaking is blocked if user has `blockWaystoneRemoval` permission (except OPs)

---

## Common Permission Setups

### Default Player
No special permissions. Can:
- Place and break their own waystones (unlimited)
- Create public or private waystones
- See public waystones and their own private waystones
- Teleport to visible waystones

### Limited Player
```
/perm group add default hytale.command.waystones.maxWaystones.3
```
All players limited to 3 waystones. Good for resource management on public servers.

### Restricted Player
```
/perm user add <uuid> hytale.command.waystones.blockPublicWaystoneCreation
/perm user add <uuid> hytale.command.waystones.maxWaystones.5
```
Can only create private waystones, limited to 5 total. Useful for preventing public waystone spam while still allowing personal use.

### View-Only Player
```
/perm user add <uuid> hytale.command.waystones.blockWaystonePlacement
/perm user add <uuid> hytale.command.waystones.blockWaystoneRemoval
```
Can use existing waystones but cannot place or break any.

### VIP Player
```
/perm group add VIP hytale.command.waystones.maxWaystones.20
```
VIP members get 20 waystones (overrides lower default group limits).

### Moderator
```
/perm user add <uuid> hytale.command.waystones.allowListMenu
/perm user add <uuid> hytale.command.waystones.allowSeeAllPrivate
/perm user add <uuid> hytale.command.waystones.allowPrivateWaystoneRemoval
```
Can view all waystones, teleport anywhere, and remove problematic private waystones. Still subject to maxWaystones limits unless also given a high limit.

### Administrator
```
/perm user addgroup <uuid> OP
```
Full access to all features. Can edit any waystone, break server-owned waystones, bypass all restrictions including waystone limits.

---

## Permission Commands Reference

```bash
# Add permission to user
/perm user add <uuid> <permission>

# Remove permission from user
/perm user remove <uuid> <permission>

# Add user to OP group
/perm user addgroup <uuid> OP

# Remove user from OP group
/perm user removegroup <uuid> OP

# Add permission to a group
/perm group add <group> <permission>

# Remove permission from a group
/perm group remove <group> <permission>
```
