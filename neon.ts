import { defineConfig } from "@neon/config/v1";

export default defineConfig({
  // Declare your Neon services here
  auth: false,
  // Almacenamiento privado de planos, documentos y renders (HU-04).
  // Cada rama hereda el bucket y sus objetos mediante copy-on-write.
  buckets: {
    "constructora-documents": { access: "private" },
  },
  // Branch policy: per-branch tuning
  branch: (branch) => {
    if (branch.isDefault) {
      // Default branch: no overrides, uses project defaults
      return {};
    }
    if (!branch.exists) {
      // New non-default branches: auto-expire
      // Run `neon checkout <name>` to create a new branch with these settings
      return { ttl: "7d" };
    }
    // Existing branch: no changes
    return {};
  },
});
