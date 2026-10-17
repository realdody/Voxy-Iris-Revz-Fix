# Voxy Iris Reverse-Z Fix

A small client-side compatibility patch for Voxy when used with Iris **1.11.4+** and a shader
pack (e.g. [Photon](https://github.com/sixthsurge/photon)).

## What it fixes

Iris 1.11.4 replaced its old "switch the world to forward-z while shaders are active" with a
depth transformer: the world now stays in the native reverse-z layout and pack shaders get
translated (depth reads/writes inverted, gbufferProjection undo-rev-z'd, ...).


Voxy versions built before that change still force forward-z whenever a shader pack is active, so:

- Voxy's own depth buffers/projection disagree with the framebuffer Iris renders into.
- vxDepthTexOpaque and vxDepthTexTrans contain forward-z values while Iris inverts them before
  the pack sees them, so pack code such as Photon's is_lod_terrain() classifies sky pixels as
  LOD terrain. The result is broken/duplicated sky textures and broken LOD shading.
- Voxy's injected shader patches (voxy_opaque.glsl and voxy_translucent.glsl) read
  depthtex1 / vxDepthTex* / gl_FragCoord with nobody transforming them.

<img width="2560" height="1440" alt="withoutvoxydepthfix" src="https://github.com/user-attachments/assets/ce00e5fc-75b4-4415-8cd5-d0ce9035f0c4" />


This mod makes Voxy advertise the native reverse-z state, hands the pack undo-rev-z'd
vxProj/vxViewProj matrices and runs the injected patch glsl through Iris' own
TransformPatcher.patchDepth().

<img width="2560" height="1440" alt="javaw_5SXq7vO7SV" src="https://github.com/user-attachments/assets/2dcd7ec3-2b92-4757-b4cc-acb1d3aa6f39" />


## Requirements

- Minecraft 26.2, Fabric loader
- [Voxy](https://modrinth.com/mod/voxy) 0.2.17-alpha or newer (all 26.2 builds; verified
  against 0.2.19-beta from Modrinth and the `dev` branch)
- [Iris](https://modrinth.com/mod/iris) 1.11.4 or newer
- Any shader pack that uses Voxy's shader integration for the full fix
