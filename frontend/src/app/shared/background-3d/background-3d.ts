import {
  Component,
  ElementRef,
  ViewChild,
  Inject,
  PLATFORM_ID,
  AfterViewInit,
  OnDestroy,
  HostListener,
  Output,
  EventEmitter
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';

@Component({
  selector: 'app-background-3d',
  standalone: true,
  templateUrl: './background-3d.html'
})
export class Background3dComponent implements AfterViewInit, OnDestroy {
  @ViewChild('deskCanvas', { static: false }) deskCanvasRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('bgCanvas', { static: false }) bgCanvasRef!: ElementRef<HTMLCanvasElement>;

  @Output() modelsLoaded = new EventEmitter<void>();

  private readonly isBrowser: boolean;
  private destroyed = false;
  private scrollY = 0;
  private animationId: number | null = null;

  // Desk scene
  private deskRenderer: THREE.WebGLRenderer | null = null;
  private deskScene: THREE.Scene | null = null;
  private deskCamera: THREE.PerspectiveCamera | null = null;
  private deskGroup: THREE.Group | null = null;
  private pcScene: THREE.Scene | null = null;
  private pcGroup: THREE.Group | null = null;
  private pcGlow: THREE.Sprite | null = null;
  private pcGlowMaterial: THREE.SpriteMaterial | null = null;
  private pcGlowBaseScale = 2.0;
  private pcStartPos = new THREE.Vector3();
  private deskInitialY = 0;

  // Background scene
  private bgRenderer: THREE.WebGLRenderer | null = null;
  private bgScene: THREE.Scene | null = null;
  private bgCamera: THREE.PerspectiveCamera | null = null;
  private bgModels: THREE.Group[] = [];

  // Animation config
  private readonly sceneRotationY = -Math.PI / 2;

  // Camera animation limits (populated after load)
  private cameraCeilingY = 0;
  private cameraGroundY = 0;
  private cameraDistance = 0;
  private deskFrontZ = 0;
  private roomHeight = 0;

  // Scroll section length: 1.5vh for the camera fly-through
  private readonly deskSectionVh = 1.6;
  private readonly pcStickyVh = 0.7;

  // Background parallax configs
  private readonly modelConfigs = [
    { path: 'models/playstation_5_controller.glb', targetSize: 2.2, x: -4.5, parallaxSpeed: 0.9, scrollOffset: 0.6, baseY: -2.5, baseRotation: 1.0 },
    { path: 'models/razer_huntsman_mini_keyboard.glb', targetSize: 2.4, x: 4.5, parallaxSpeed: 1.1, scrollOffset: 0.9, baseY: -1.0, baseRotation: -1.0 }
  ];

  constructor(@Inject(PLATFORM_ID) private readonly platformId: object) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  @HostListener('window:scroll', [])
  onWindowScroll(): void {
    if (this.isBrowser) {
      this.scrollY = window.scrollY;
    }
  }

  async ngAfterViewInit(): Promise<void> {
    if (!this.isBrowser) return;

    this.initDeskThree();
    this.initBgThree();
    await this.loadDeskScene();
    await this.loadBackgroundModels();
    this.modelsLoaded.emit();
    this.tick();
  }

  private initDeskThree(): void {
    const canvas = this.deskCanvasRef.nativeElement;
    const width = window.innerWidth;
    const height = window.innerHeight;

    this.deskScene = new THREE.Scene();
    this.deskCamera = new THREE.PerspectiveCamera(50, width / height, 0.1, 1000);

    this.deskRenderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
    this.deskRenderer.setSize(width, height);
    this.deskRenderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    this.deskRenderer.outputColorSpace = THREE.SRGBColorSpace;
    this.deskRenderer.toneMapping = THREE.ACESFilmicToneMapping;
    this.deskRenderer.toneMappingExposure = 1.0;

    const ambient = new THREE.AmbientLight(0xffffff, 1.2);
    this.deskScene.add(ambient);
    const dir = new THREE.DirectionalLight(0xffffff, 2);
    dir.position.set(5, 10, 5);
    this.deskScene.add(dir);
    const frontLight = new THREE.DirectionalLight(0xffffff, 1.5);
    frontLight.position.set(0, 5, 15);
    this.deskScene.add(frontLight);

    this.pcScene = new THREE.Scene();
    this.pcScene.add(ambient.clone());
    this.pcScene.add(dir.clone());
    this.pcScene.add(frontLight.clone());

    // Create soft radial glow behind the PC
    const canvas2d = document.createElement('canvas');
    canvas2d.width = 256;
    canvas2d.height = 256;
    const ctx = canvas2d.getContext('2d');
    if (ctx) {
      const gradient = ctx.createRadialGradient(128, 128, 0, 128, 128, 128);
      gradient.addColorStop(0, 'rgba(110, 60, 255, 0.4)'); // Similar to primary color
      gradient.addColorStop(1, 'rgba(110, 60, 255, 0)');
      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, 256, 256);
      const glowTexture = new THREE.CanvasTexture(canvas2d);
      
      this.pcGlowMaterial = new THREE.SpriteMaterial({ map: glowTexture, transparent: true, opacity: 0, depthWrite: false });
      this.pcGlow = new THREE.Sprite(this.pcGlowMaterial);
      this.pcGlow.scale.set(5, 5, 1);
      this.pcScene.add(this.pcGlow);
    }
  }

  private initBgThree(): void {
    const canvas = this.bgCanvasRef.nativeElement;
    const width = window.innerWidth;
    const height = window.innerHeight;

    this.bgScene = new THREE.Scene();
    this.bgCamera = new THREE.PerspectiveCamera(50, width / height, 0.1, 1000);
    this.bgCamera.position.set(0, 0, 10);
    this.bgCamera.lookAt(0, 0, 0);

    this.bgRenderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
    this.bgRenderer.setSize(width, height);
    this.bgRenderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    this.bgRenderer.outputColorSpace = THREE.SRGBColorSpace;
    this.bgRenderer.toneMapping = THREE.ACESFilmicToneMapping;
    this.bgRenderer.toneMappingExposure = 1.0;

    const ambient = new THREE.AmbientLight(0xffffff, 1.2);
    this.bgScene.add(ambient);
    const dir = new THREE.DirectionalLight(0xffffff, 2);
    dir.position.set(5, 10, 5);
    this.bgScene.add(dir);
    const frontLight = new THREE.DirectionalLight(0xffffff, 1.5);
    frontLight.position.set(0, 5, 15);
    this.bgScene.add(frontLight);
  }

  private async loadDeskScene(): Promise<void> {
    const loader = new GLTFLoader();
    try {
      const gltf = await loader.loadAsync('models/desk_scene.glb');
      const root = gltf.scene;
      
      let deskNode: THREE.Object3D | undefined;
      let pcNode: THREE.Object3D | undefined;
      root.traverse((obj) => {
        if (obj.name === 'Sketchfab_model') deskNode = obj;
        if (obj.name === 'Sketchfab_model001') pcNode = obj;
      });

      if (!deskNode || !pcNode) {
        const allNames: string[] = [];
        root.traverse((obj) => allNames.push(obj.name));
        console.error('Could not find desk or PC in desk_scene.glb. Names found:', allNames.slice(0, 30));
        return;
      }

      const sceneGroup = new THREE.Group();
      sceneGroup.add(root);
      this.deskScene?.add(sceneGroup);

      // Scale and rotate the whole room
      const fullBox = new THREE.Box3().setFromObject(root);
      const fullSize = fullBox.getSize(new THREE.Vector3());
      const targetHeight = 7.5;
      const scale = fullSize.y > 0 ? targetHeight / fullSize.y : 1;
      sceneGroup.scale.setScalar(scale);
      sceneGroup.rotation.y = this.sceneRotationY;
      sceneGroup.updateMatrixWorld(true);

      // Center at world origin
      const centeredBox = new THREE.Box3().setFromObject(sceneGroup);
      const centeredCenter = centeredBox.getCenter(new THREE.Vector3());
      sceneGroup.position.sub(centeredCenter);
      sceneGroup.updateMatrixWorld(true);

      // Detach PC
      const pcWorldPos = new THREE.Vector3();
      pcNode.getWorldPosition(pcWorldPos);

      this.pcGroup = new THREE.Group();
      this.pcGroup.position.copy(pcWorldPos);
      this.pcGroup.rotation.y = this.sceneRotationY;
      this.pcScene?.add(this.pcGroup);
      this.pcGroup.attach(pcNode);

      // Store the group directly without detaching the PC
      this.deskGroup = sceneGroup;
      this.deskInitialY = this.deskGroup.position.y;
      this.pcStartPos.copy(pcWorldPos);

      const pcBox = new THREE.Box3().setFromObject(this.pcGroup);
      const pcSize = pcBox.getSize(new THREE.Vector3());
      this.pcGlowBaseScale = Math.max(pcSize.x, pcSize.y) * 1.8;

      // Compute camera limits from the room geometry
      const roomBox = new THREE.Box3().setFromObject(this.deskGroup);
      const roomSize = roomBox.getSize(new THREE.Vector3());
            this.roomHeight = roomSize.y;

      const deskWorldBox = new THREE.Box3().setFromObject(deskNode);
      const deskSize = deskWorldBox.getSize(new THREE.Vector3());
      const deskBottomY = deskWorldBox.min.y;

      const fovRad = THREE.MathUtils.degToRad(this.deskCamera!.fov);
      this.cameraDistance = Math.max(
        (roomSize.y * 0.55) / (2 * Math.tan(fovRad / 2)),
        roomSize.z * 0.4 + 1.0
      );

      this.cameraCeilingY = roomBox.max.y - roomSize.y * 0.45;
      this.cameraGroundY = Math.max(
        deskBottomY - deskSize.y * 0.1,
        roomBox.min.y + roomSize.y * 0.375
      );
      this.deskFrontZ = roomBox.max.z + 0.5;

      if (this.deskCamera) {
        this.deskCamera.position.set(0, this.cameraCeilingY, this.cameraDistance);
        this.deskCamera.lookAt(0, 0, 0);
      }
    } catch (error) {
      console.error('Error loading desk_scene.glb:', error);
    }
  }

  private async loadBackgroundModels(): Promise<void> {
    const loader = new GLTFLoader();
    for (const config of this.modelConfigs) {
      try {
        const gltf = await loader.loadAsync(config.path);
        const model = gltf.scene;

        const box = new THREE.Box3().setFromObject(model);
        const size = box.getSize(new THREE.Vector3()).length();
        const center = box.getCenter(new THREE.Vector3());
        model.position.sub(center);

        if (size > 0) {
          model.scale.setScalar(config.targetSize / size);
        }

        const pivot = new THREE.Group();
        pivot.add(model);
        pivot.position.x = config.x;
        this.bgScene?.add(pivot);
        this.bgModels.push(pivot);
      } catch (error) {
        console.error(`Error loading model ${config.path}:`, error);
      }
    }
  }

  private getViewportHeightAtZ(camera: THREE.PerspectiveCamera, targetZ: number): number {
    const fovRad = THREE.MathUtils.degToRad(camera.fov);
    const dist = Math.abs(camera.position.z - targetZ);
    return 2 * Math.tan(fovRad / 2) * dist;
  }

  private tick = (): void => {
    if (this.destroyed) return;

    const vh = window.innerHeight * 1.2;
    const deskSectionPx = vh * this.deskSectionVh;
    const pageHeight = document.documentElement.scrollHeight - vh;
    const scrollPercent = pageHeight > 0 ? this.scrollY / pageHeight : 0;

    const rawProgress = this.scrollY / deskSectionPx;
    const progress = Math.min(Math.max(rawProgress, 0), 1);

    // ---- DESK SCENE ----
    if (this.deskGroup && this.deskCamera && this.pcGroup) {
      if (progress < 1) {
        this.deskCamera.clearViewOffset();
        // Phase 1: Camera moves down from ceiling to ground and zooms in slightly
        const camY = THREE.MathUtils.lerp(this.cameraCeilingY, this.cameraGroundY, progress);
        const camZ = THREE.MathUtils.lerp(this.cameraDistance, this.cameraDistance * 0.8, progress);
        this.deskCamera.position.set(0, camY, camZ);
        this.deskCamera.lookAt(0, 0, 0);
        
        // Desk stays still
        this.deskGroup.position.y = this.deskInitialY;

        // PC Transform
        // Smoother transition: use an easing function so it stops gracefully at progress = 1
        const easeProgress = Math.sin((progress * Math.PI) / 2); // easeOutSine
        
        // Scale it bigger
        const currentScale = THREE.MathUtils.lerp(1, 2.0, easeProgress);
        this.pcGroup.scale.setScalar(currentScale);
        
        // Rotate towards us and tilt to reveal the top
        this.pcGroup.rotation.y = this.sceneRotationY + easeProgress * Math.PI;
        this.pcGroup.rotation.x = easeProgress * 0.4;

        // Move it up and slightly forward so it doesn't clip through the desk when scaling/rotating
        const lift = easeProgress * 0.6;
        const moveZ = easeProgress * 0.5;
        this.pcGroup.position.set(
          this.pcStartPos.x,
          this.pcStartPos.y + lift,
          this.pcStartPos.z + moveZ
        );

        if (this.pcGlow && this.pcGlowMaterial && this.pcGroup) {
          this.pcGlowMaterial.opacity = easeProgress * 0.7;
          this.pcGlow.scale.set(this.pcGlowBaseScale * currentScale, this.pcGlowBaseScale * currentScale, 1);
          
          const currentBox = new THREE.Box3().setFromObject(this.pcGroup);
          const center = currentBox.getCenter(new THREE.Vector3());
          this.pcGlow.position.set(center.x, center.y + 0.6, center.z - 2.0);
        }

      } else {
        // Phase 2: Camera stays at ground, desk stays fixed
        this.deskCamera.position.set(0, this.cameraGroundY, this.cameraDistance * 0.8);
        this.deskCamera.lookAt(0, 0, 0);
        this.deskGroup.position.y = this.deskInitialY;

        const overScroll = this.scrollY - deskSectionPx;
        // Finish the animation much faster (over 0.6vh instead of the full 1.5vh sticky duration)
        // so the PC is correctly positioned as soon as the hero section is fully in view
        const stickyAnimationPx = vh * 0.8;
        const stickyRawProgress = overScroll / stickyAnimationPx;
        const stickyProgress = Math.min(Math.max(stickyRawProgress, 0), 1);
        
        // Easing curve: slow start, fast middle, slow end (sine ease-in-out)
        const easeInOut = (1 - Math.cos(Math.PI * stickyProgress)) / 2;

        const isDesktop = window.innerWidth >= 1024;
        const targetOffsetX = isDesktop ? 1 : 0;
        const targetOffsetY = isDesktop ? -0.3 : -1.3;

        const currentX = this.pcStartPos.x + (targetOffsetX * easeInOut);
        const currentY = this.pcStartPos.y + 0.6 + ((targetOffsetY - 0.6) * easeInOut);

        this.pcGroup.scale.setScalar(2.0);
        this.pcGroup.rotation.y = this.sceneRotationY + Math.PI;
        this.pcGroup.rotation.x = 0.4;
        this.pcGroup.position.set(
          currentX,
          currentY,
          this.pcStartPos.z + 0.5
        );

        if (this.pcGlow && this.pcGlowMaterial && this.pcGroup) {
          this.pcGlowMaterial.opacity = 0.7;
          this.pcGlow.scale.set(this.pcGlowBaseScale * 2.0, this.pcGlowBaseScale * 2.0, 1);
          
          const currentBox = new THREE.Box3().setFromObject(this.pcGroup);
          const center = currentBox.getCenter(new THREE.Vector3());
          this.pcGlow.position.set(center.x, center.y + 0.6, center.z - 2.0);
        }
      }
    }

    // ---- BACKGROUND PARALLAX SCENE ----
    this.bgModels.forEach((model, index) => {
      const config = this.modelConfigs[index];
      const offsetFromAnchor = (scrollPercent - config.scrollOffset) * pageHeight;
      const worldUnitsPerViewport = 4;
      model.position.y = config.baseY + (offsetFromAnchor / vh) * config.parallaxSpeed * worldUnitsPerViewport;
      model.rotation.y = config.baseRotation + this.scrollY * 0.001 + this.sceneRotationY;
      model.rotation.x = 0;
      model.rotation.z = 0;
    });

    if (this.deskRenderer && this.deskScene && this.deskCamera) {
      if (progress >= 1) {
        const overScroll = this.scrollY - deskSectionPx;
        this.deskCamera.setViewOffset(
          window.innerWidth, window.innerHeight,
          0, overScroll,
          window.innerWidth, window.innerHeight
        );
      } else {
        this.deskCamera.clearViewOffset();
      }
      this.deskRenderer.render(this.deskScene, this.deskCamera);
    }
    
    if (this.bgRenderer && this.bgScene && this.bgCamera) {
      this.bgRenderer.autoClear = false;
      this.bgRenderer.clear();
      this.bgRenderer.render(this.bgScene, this.bgCamera);
      this.bgRenderer.clearDepth();
      
      if (this.pcScene && this.deskCamera) {
        if (progress >= 1) {
          const overScroll = this.scrollY - deskSectionPx;
          const pcStickyPx = vh * this.pcStickyVh;
          const pcOverScroll = Math.max(0, overScroll - pcStickyPx);
          
          if (pcOverScroll > 0) {
            this.deskCamera.setViewOffset(
              window.innerWidth, window.innerHeight,
              0, pcOverScroll,
              window.innerWidth, window.innerHeight
            );
          } else {
            this.deskCamera.clearViewOffset();
          }
        } else {
          this.deskCamera.clearViewOffset();
        }
        this.bgRenderer.render(this.pcScene, this.deskCamera);
      }
    }

    this.animationId = requestAnimationFrame(this.tick);
  };

  @HostListener('window:resize', [])
  onResize(): void {
    if (!this.deskRenderer || !this.deskCamera || !this.bgRenderer || !this.bgCamera) return;

    const width = window.innerWidth;
    const height = window.innerHeight;

    this.deskCamera.aspect = width / height;
    this.deskCamera.updateProjectionMatrix();
    this.deskRenderer.setSize(width, height);

    this.bgCamera.aspect = width / height;
    this.bgCamera.updateProjectionMatrix();
    this.bgRenderer.setSize(width, height);
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    if (this.animationId) {
      cancelAnimationFrame(this.animationId);
    }
    this.deskRenderer?.dispose();
    this.bgRenderer?.dispose();
  }
}
