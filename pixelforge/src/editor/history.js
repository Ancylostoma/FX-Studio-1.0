// Historial de deshacer/rehacer. Guarda instantáneas serializadas (las
// escenas son JSON compacto, así que el coste en memoria es pequeño).
export class History {
  constructor(limit = 60) { this.limit = limit; this.stack = []; this.index = -1; }
  push(state) {
    const snap = JSON.stringify(state);
    if (this.index >= 0 && this.stack[this.index] === snap) return false;
    this.stack.length = this.index + 1;
    this.stack.push(snap);
    if (this.stack.length > this.limit) this.stack.shift();
    this.index = this.stack.length - 1;
    return true;
  }
  get canUndo() { return this.index > 0; }
  get canRedo() { return this.index < this.stack.length - 1; }
  undo() { return this.canUndo ? JSON.parse(this.stack[--this.index]) : null; }
  redo() { return this.canRedo ? JSON.parse(this.stack[++this.index]) : null; }
  reset(state) { this.stack = []; this.index = -1; if (state !== undefined) this.push(state); }
}
