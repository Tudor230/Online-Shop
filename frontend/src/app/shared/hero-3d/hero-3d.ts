import {
  Component,
  ElementRef,
  ViewChild,
  Inject,
  PLATFORM_ID,
  AfterViewInit,
  OnDestroy
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import type { Mesh, Group, Object3D, LineSegments, Line, WebGLRenderer, Scene, PerspectiveCamera, MeshStandardMaterial } from 'three';

@Component({
  selector: 'app-hero-3d',
  standalone: true,
  templateUrl: './hero-3d.html',
  styleUrl: './hero-3d.css'
})
export class Hero3dComponent implements AfterViewInit, OnDestroy {
  @ViewChild('canvas', { static: false }) canvasRef!: ElementRef<HTMLCanvasElement>;

  private readonly isBrowser: boolean;
  private animationId: number | null = null;
  private renderer: WebGLRenderer | null = null;
  private scene: Scene | null = null;
  private camera: PerspectiveCamera | null = null;
  private meshes: (Mesh | Group)[] = [];
  private resizeObserver: ResizeObserver | null = null;
  private prefersReducedMotion = false;
  private readonly mouse = { x: 0, y: 0 };
  private readonly targetRotation = { x: 0, y: 0 };
  private destroyed = false;
  private readonly cleanupFns: (() => void)[] = [];
  private edgeLines: LineSegments[] = [];
  private connectingLines: Line[] = [];

  constructor(@Inject(PLATFORM_ID) private readonly platformId: object) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  async ngAfterViewInit(): Promise<void> {
    if (!this.isBrowser) {
      return;
    }

    const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    this.prefersReducedMotion = motionQuery.matches;

    try {
      const THREE_MODULE: typeof import('three') = await import('three');

      const canvas = this.canvasRef.nativeElement;
      const width = canvas.clientWidth;
      const height = canvas.clientHeight;

      if (width === 0 || height === 0) {
        return;
      }

      this.scene = new THREE_MODULE.Scene();

      this.camera = new THREE_MODULE.PerspectiveCamera(45, width / height, 0.1, 100);
      this.camera.position.z = 8;

      this.renderer = new THREE_MODULE.WebGLRenderer({
        canvas,
        antialias: true,
        alpha: true
      });
      this.renderer.setSize(width, height);
      this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

      const ambientLight = new THREE_MODULE.AmbientLight(0xffffff, 0.6);
      this.scene.add(ambientLight);

      const dirLight = new THREE_MODULE.DirectionalLight(0xffffff, 1.2);
      dirLight.position.set(5, 5, 5);
      this.scene.add(dirLight);

      const fillLight = new THREE_MODULE.DirectionalLight(0xaaaaff, 0.5);
      fillLight.position.set(-5, 0, 2);
      this.scene.add(fillLight);

      const material1 = new THREE_MODULE.MeshStandardMaterial({
        color: 'hsl(250, 85%, 55%)',
        roughness: 0.3,
        metalness: 0.1,
        transparent: true,
        opacity: 0.6,
        polygonOffset: true,
        polygonOffsetFactor: 1,
        polygonOffsetUnits: 1
      });
      const material2 = new THREE_MODULE.MeshStandardMaterial({
        color: 'hsl(270, 80%, 75%)',
        roughness: 0.4,
        metalness: 0.1,
        transparent: true,
        opacity: 0.6,
        polygonOffset: true,
        polygonOffsetFactor: 1,
        polygonOffsetUnits: 1
      });
      const material3 = new THREE_MODULE.MeshStandardMaterial({
        color: 'hsl(260, 70%, 60%)',
        roughness: 0.3,
        metalness: 0.2,
        transparent: true,
        opacity: 0.6,
        polygonOffset: true,
        polygonOffsetFactor: 1,
        polygonOffsetUnits: 1
      });

      const gameController = this.createGameController(THREE_MODULE, material1);
      gameController.position.set(-1.5, 0, 0);
      gameController.rotation.set(0.1, -0.3, 0);
      this.scene.add(gameController);
      this.meshes.push(gameController);

      const keyboard = this.createKeyboard(THREE_MODULE, material2);
      keyboard.position.set(0.5, -0.3, 0.3);
      keyboard.rotation.set(0.15, 0.2, 0);
      this.scene.add(keyboard);
      this.meshes.push(keyboard);

      const headset = this.createHeadset(THREE_MODULE, material3);
      headset.position.set(1.2, 0.5, -0.3);
      headset.rotation.set(0, 0.2, 0.1);
      this.scene.add(headset);
      this.meshes.push(headset);

      this.addEdgeLines(THREE_MODULE, gameController, 'hsl(270, 80%, 85%)');
      this.addEdgeLines(THREE_MODULE, keyboard, 'hsl(270, 80%, 85%)');
      this.addEdgeLines(THREE_MODULE, headset, 'hsl(270, 80%, 85%)');

      const connectionPoints = new Float32Array([
        -1.5, 0, 0,
        0.5, -0.3, 0.3,
        1.2, 0.5, -0.3,
        -1.5, 0, 0
      ]);
      const connectionGeo = new THREE_MODULE.BufferGeometry();
      connectionGeo.setAttribute(
        'position',
        new THREE_MODULE.BufferAttribute(connectionPoints, 3)
      );
      const connectionMat = new THREE_MODULE.LineBasicMaterial({
        color: 'hsl(250, 85%, 55%)',
        transparent: true,
        opacity: 0.25
      });
      const connectionLine = new THREE_MODULE.Line(connectionGeo, connectionMat);
      this.scene.add(connectionLine);
      this.connectingLines.push(connectionLine);

      const onMouseMove = (e: MouseEvent): void => {
        this.mouse.x = (e.clientX / window.innerWidth) * 2 - 1;
        this.mouse.y = -(e.clientY / window.innerHeight) * 2 + 1;
      };
      window.addEventListener('mousemove', onMouseMove);
      this.cleanupFns.push(() => window.removeEventListener('mousemove', onMouseMove));

      this.resizeObserver = new ResizeObserver((entries) => {
        for (const entry of entries) {
          const { width: w, height: h } = entry.contentRect;
          if (this.camera && this.renderer) {
            this.camera.aspect = w / h;
            this.camera.updateProjectionMatrix();
            this.renderer.setSize(w, h);
          }
        }
      });
      this.resizeObserver.observe(canvas);
      this.cleanupFns.push(() => this.resizeObserver?.disconnect());

      const render = (): void => {
        if (this.renderer && this.scene && this.camera) {
          this.renderer.render(this.scene, this.camera);
        }
      };

      const tick = (): void => {
        if (this.destroyed) {
          return;
        }

        if (!this.prefersReducedMotion) {
          this.meshes.forEach((mesh, i) => {
            mesh.rotation.y += 0.001 * (i % 2 === 0 ? 1 : -1);
          });

          this.targetRotation.x = this.mouse.y * (15 * (Math.PI / 180));
          this.targetRotation.y = this.mouse.x * (15 * (Math.PI / 180));

          if (this.camera) {
            this.camera.rotation.x += (this.targetRotation.x - this.camera.rotation.x) * 0.05;
            this.camera.rotation.y += (this.targetRotation.y - this.camera.rotation.y) * 0.05;
          }
        }

        render();

        if (this.prefersReducedMotion) {
          this.animationId = null;
          return;
        }

        this.animationId = requestAnimationFrame(tick);
      };

      const onMotionChange = (e: MediaQueryListEvent): void => {
        this.prefersReducedMotion = e.matches;

        if (!this.prefersReducedMotion) {
          if (this.animationId === null && !this.destroyed) {
            tick();
          }
        }
      };
      motionQuery.addEventListener('change', onMotionChange);
      this.cleanupFns.push(() => motionQuery.removeEventListener('change', onMotionChange));

      if (this.prefersReducedMotion) {
        render();
      } else {
        tick();
      }
    } catch {
      // WebGL initialization failed; CSS fallback remains visible beneath the canvas.
    }
  }

  private createGameController(THREE_MODULE: typeof import('three'), material: MeshStandardMaterial): Group {
    const group = new THREE_MODULE.Group();

    const body = new THREE_MODULE.BoxGeometry(1.2, 0.4, 0.6);
    const bodyMesh = new THREE_MODULE.Mesh(body, material);
    group.add(bodyMesh);

    const leftGrip = new THREE_MODULE.SphereGeometry(0.25, 16, 16);
    const leftGripMesh = new THREE_MODULE.Mesh(leftGrip, material);
    leftGripMesh.position.set(-0.7, -0.1, 0);
    group.add(leftGripMesh);

    const rightGrip = new THREE_MODULE.SphereGeometry(0.25, 16, 16);
    const rightGripMesh = new THREE_MODULE.Mesh(rightGrip, material);
    rightGripMesh.position.set(0.7, -0.1, 0);
    group.add(rightGripMesh);

    const btnGeo = new THREE_MODULE.CylinderGeometry(0.08, 0.08, 0.1, 8);
    const btn1 = new THREE_MODULE.Mesh(btnGeo, material);
    btn1.position.set(0.3, 0.25, 0.2);
    btn1.rotation.x = Math.PI / 2;
    group.add(btn1);

    const btn2 = new THREE_MODULE.Mesh(btnGeo, material);
    btn2.position.set(0.5, 0.25, 0);
    btn2.rotation.x = Math.PI / 2;
    group.add(btn2);

    return group;
  }

  private createKeyboard(THREE_MODULE: typeof import('three'), material: MeshStandardMaterial): Group {
    const group = new THREE_MODULE.Group();

    const base = new THREE_MODULE.BoxGeometry(1.4, 0.08, 0.5);
    const baseMesh = new THREE_MODULE.Mesh(base, material);
    baseMesh.position.y = -0.04;
    group.add(baseMesh);

    const keyGeo = new THREE_MODULE.BoxGeometry(0.08, 0.04, 0.06);
    const keyRows = 4;
    const keyCols = 10;
    for (let row = 0; row < keyRows; row++) {
      for (let col = 0; col < keyCols; col++) {
        const key = new THREE_MODULE.Mesh(keyGeo, material);
        key.position.set(
          -0.5 + col * 0.11,
          0.02,
          -0.2 + row * 0.13
        );
        group.add(key);
      }
    }

    return group;
  }

  private createHeadset(THREE_MODULE: typeof import('three'), material: MeshStandardMaterial): Group {
    const group = new THREE_MODULE.Group();

    const earCupGeo = new THREE_MODULE.CylinderGeometry(0.25, 0.25, 0.15, 16);
    const leftEarCup = new THREE_MODULE.Mesh(earCupGeo, material);
    leftEarCup.rotation.z = Math.PI / 2;
    leftEarCup.position.set(-0.35, 0, 0);
    group.add(leftEarCup);

    const rightEarCup = new THREE_MODULE.Mesh(earCupGeo, material);
    rightEarCup.rotation.z = Math.PI / 2;
    rightEarCup.position.set(0.35, 0, 0);
    group.add(rightEarCup);

    const bandGeo = new THREE_MODULE.TorusGeometry(0.35, 0.04, 8, 16, Math.PI);
    const band = new THREE_MODULE.Mesh(bandGeo, material);
    band.position.y = 0.25;
    band.rotation.x = Math.PI;
    group.add(band);

    return group;
  }

  private addEdgeLines(THREE_MODULE: typeof import('three'), group: Group, color: string): void {
    group.traverse((child: Object3D) => {
      if (child instanceof THREE_MODULE.Mesh && child.geometry) {
        const edges = new THREE_MODULE.EdgesGeometry(child.geometry);
        const lines = new THREE_MODULE.LineSegments(
          edges,
          new THREE_MODULE.LineBasicMaterial({
            color,
            transparent: true,
            opacity: 0.9
          })
        );
        lines.position.copy(child.position);
        lines.rotation.copy(child.rotation);
        if (child.parent) {
          lines.position.add(child.parent.position);
        }
        this.scene?.add(lines);
        this.edgeLines.push(lines);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroyed = true;

    if (this.animationId !== null) {
      cancelAnimationFrame(this.animationId);
    }

    this.cleanupFns.forEach((fn) => fn());

    this.meshes.forEach((obj) => {
      if ((obj as Group).isGroup) {
        (obj as Group).traverse((child) => {
          if ((child as Mesh).isMesh) {
            (child as Mesh).geometry.dispose();
            const material = (child as Mesh).material;
            if (Array.isArray(material)) {
              material.forEach((m) => m.dispose());
            } else {
              material.dispose();
            }
          }
        });
      } else {
        const mesh = obj as Mesh;
        mesh.geometry.dispose();
        const material = mesh.material;
        if (Array.isArray(material)) {
          material.forEach((m) => m.dispose());
        } else {
          material.dispose();
        }
      }
    });
    this.meshes = [];

    this.edgeLines.forEach((line) => {
      line.geometry.dispose();
      const material = line.material;
      if (Array.isArray(material)) {
        material.forEach((m) => m.dispose());
      } else {
        material.dispose();
      }
    });
    this.edgeLines = [];

    this.connectingLines.forEach((line) => {
      line.geometry.dispose();
      const material = line.material;
      if (Array.isArray(material)) {
        material.forEach((m) => m.dispose());
      } else {
        material.dispose();
      }
    });
    this.connectingLines = [];

    this.renderer?.dispose();
    this.renderer = null;
    this.scene = null;
    this.camera = null;
  }
}
