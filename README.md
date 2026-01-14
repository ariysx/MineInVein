# ⛏️ Mine in Vein

> **Break one ore, mine the entire vein.**

A quality-of-life Hytale mod that adds intelligent vein mining while maintaining game balance through durability costs.

---

## 📖 Overview

Mine in Vein eliminates the tedious task of manually breaking every ore block in a vein. Simply crouch and break one ore block, and the mod will automatically mine all connected ore blocks of the same type - up to 64 blocks in a single action.

## ✨ Features

- **🎯 Smart Detection** - Automatically finds and breaks all connected ore blocks
- **⚡ Crouch Activation** - Hold crouch while mining to trigger vein mining
- **🔧 Durability Management** - Intelligently limits blocks mined based on tool durability
- **⛏️ Pickaxe Only** - Works exclusively with pickaxe tools for balance
- **📦 Full Item Drops** - All mined blocks drop their items as normal
- **⚙️ Optimized Performance** - Efficient flood-fill algorithm with custom position hashing
- **🎮 Balanced** - Fair durability cost prevents exploitation

## 🚀 Usage

1. Equip any pickaxe
2. Hold crouch/sneak (default: Shift)
3. Break an ore block
4. Watch the entire vein disappear instantly!

**Note:** Your tool loses durability for each block mined. If your pickaxe has 10 durability remaining but the vein has 30 blocks, only 10 blocks will be mined.

## ⚙️ Technical Details

- **Max Blocks:** 64 per activation
- **Algorithm:** Breadth-first search (BFS) flood fill
- **Position Tracking:** Custom 63-bit hash supporting ±1M coordinate range
- **Performance:** Pre-sized collections to minimize memory allocations

## 📦 Installation

1. Download the latest release
2. Place the `.jar` file in your Hytale mods folder
3. Launch Hytale and enjoy effortless vein mining!

Built using the Hytale ECS (Entity Component System) architecture.

## 🤝 Contributing

Contributions are welcome! Feel free to open issues or submit pull requests.

---

**Made with ⛏️ for the Hytale community**