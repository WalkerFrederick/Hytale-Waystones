# Waystones Plugin - Manual Test Cases

Pre-release checklist for manual testing.

---

## Basic Functionality

- [ ] Place a waystone block
- [ ] Right-click waystone to open naming dialog
- [ ] Name the waystone and confirm creation
- [ ] Cancel waystone creation (block should remain, no waystone registered)
- [ ] Right-click existing waystone to open list
- [ ] Teleport to another waystone
- [ ] Verify arrival banner shows waystone name
- [ ] Break a waystone you own
- [ ] Verify waystone is removed from registry after breaking

---

## Waystone Settings

- [ ] Open settings page (gear icon)
- [ ] Change waystone name
- [ ] Change visibility (public/private)
- [ ] Change teleport direction (north/south/east/west)
- [ ] Change player orientation (towards/away)
- [ ] Verify teleport position matches settings

---

## Public vs Private Waystones

- [ ] Create a public waystone
- [ ] Create a private waystone
- [ ] Verify public waystones appear in Public tab for all players
- [ ] Verify private waystones only appear for owner
- [ ] Verify private waystone shows warning icon when viewing others' private (with permission)

---

## Cross-World Teleport

- [ ] Create waystones in two different worlds
- [ ] Teleport from world A to world B
- [ ] Verify player arrives at correct location
- [ ] Verify arrival banner displays

---

## Permissions - Normal Player (no special perms)

- [ ] Can place waystones
- [ ] Can break own waystones
- [ ] Cannot break others' private waystones
- [ ] Cannot break server-owned waystones
- [ ] Can create public and private waystones
- [ ] Cannot see others' private waystones
- [ ] Cannot access /waystones list command

---

## Permissions - OP Player

- [ ] Can break any waystone (including server-owned)
- [ ] Can see all private waystones
- [ ] Can edit any waystone (gear icon visible)
- [ ] Can access Priority setting
- [ ] Can access Server Owned setting
- [ ] Can access Reset (delete) button
- [ ] Can use /waystones list command
- [ ] Bypasses all "block" permissions

---

## Permissions - Block Permissions

### blockWaystonePlacement
- [ ] Player with permission cannot place waystone blocks

### blockWaystoneRemoval
- [ ] Player with permission cannot break any waystones

### blockPublicWaystoneCreation
- [ ] New waystones default to private
- [ ] Visibility dropdown is hidden in settings
- [ ] Cannot change existing waystone to public

---

## Permissions - Allow Permissions

### allowListMenu
- [ ] Player with permission can use /waystones list

### allowEditAll
- [ ] Player can see gear icon on all waystones
- [ ] Player can access Priority/ServerOwned/Reset settings

### allowSeeAllPrivate
- [ ] Player can see all private waystones in list
- [ ] Player can teleport to any visible waystone

### allowPrivateWaystoneRemoval
- [ ] Player can break others' private waystones

---

## Permissions - maxWaystones Limit

- [ ] Player with maxWaystones.3 can create 3 waystones
- [ ] Player at limit sees "maximum waystone limit" message
- [ ] Player with multiple limits uses highest value
- [ ] OP bypasses limit entirely

---

## Server-Owned Waystones

- [ ] OP can mark waystone as server-owned
- [ ] Server-owned waystones don't show owner name
- [ ] Non-OP cannot break server-owned waystones
- [ ] Non-OP sees "do not have permission" message when trying to break

---

## Search & Tabs

- [ ] Search filters waystones by name
- [ ] Search filters waystones by owner name
- [ ] Public tab shows only public waystones
- [ ] Private tab shows only private waystones

---

## Discovery System (requireDiscover config)

### Setup
- [ ] Set `requireDiscover: true` in waystones.json Config section

### Discovery Tracking
- [ ] Create a new waystone - it should be auto-discovered by creator
- [ ] Right-click an existing waystone - it should be discovered
- [ ] Verify waystones-players.json is created with discovery data

### Discovery Filter (requireDiscover = true)
- [ ] New player only sees waystones they've discovered (and defaultDiscovered)
- [ ] Player cannot see undiscovered waystones in list
- [ ] After visiting a waystone, it appears in their list

### defaultDiscovered Setting
- [ ] OP can see "Default Discovered" dropdown in settings
- [ ] Set waystone to defaultDiscovered = Yes
- [ ] Verify all players can see it even if not discovered

### allowShowUndiscovered Permission
- [ ] Player with permission sees all waystones (bypasses discovery)
- [ ] OP bypasses discovery filter automatically

### Discovery Filter Off (requireDiscover = false)
- [ ] Discovery is still tracked in waystones-players.json
- [ ] But all waystones are visible regardless of discovery status
- [ ] "Default Discovered" setting is hidden in settings page

---

## Waystone Color

### Color Selection (Settings Page)
- [ ] "Waystone Color" dropdown appears in settings page
- [ ] Default color is "Default (Blue)"
- [ ] Dropdown shows options: Default (Blue), Red, Green
- [ ] Changing color updates the waystone block texture immediately
- [ ] Color preference is saved in waystones.json

### Color Block Variants
- [ ] Blue waystone uses default texture and Fire_Blue particles
- [ ] Red waystone uses waystone-red.png texture and Fire_Red particles
- [ ] Green waystone uses waystone-green.png texture and Fire_Green particles
- [ ] All color variants can be interacted with normally (open menu)
- [ ] All color variants can be broken (subject to permissions)
- [ ] Breaking any color variant removes it from registry

### Color Command
- [ ] `/waystones edit "Name" color red` changes color to red
- [ ] `/waystones edit "Name" color green` changes color to green
- [ ] `/waystones edit "Name" color default` changes color back to blue
- [ ] Invalid color value gives error message

---

## Edge Cases

- [ ] Waystone with very long name (100 char limit)
- [ ] Waystone name with special characters
- [ ] Duplicate name prevention works
- [ ] Break waystone that doesn't exist in registry (orphaned block)
- [ ] Server restart preserves all waystones
- [ ] Config debugLogs toggle works

---

## Commands

- [ ] /waystones - Shows plugin info
- [ ] /waystones list - Opens menu (with permission)
- [ ] /waystones edit "name" property value - Updates property

---

## Quick Smoke Test (5 min)

1. [ ] Place waystone, name it "Test A"
2. [ ] Walk away, place another waystone "Test B"  
3. [ ] Teleport from B to A
4. [ ] Verify banner shows "Test A"
5. [ ] Break waystone A
6. [ ] Verify A is gone from list
