import type { Directive, DirectiveBinding } from 'vue';
import { usePermissionStore } from '../stores/permission';

function toggleElement(el: HTMLElement, visible: boolean) {
      const originalDisplay = el.dataset.permissionDisplay ?? '';
      if (visible) {
            if (originalDisplay) {
                  el.style.display = originalDisplay;
            } else {
                  el.style.removeProperty('display');
            }
            return;
      }

      if (el.dataset.permissionDisplay === undefined) {
            el.dataset.permissionDisplay = el.style.display || '';
      }
      el.style.display = 'none';
}

export const permission: Directive = {
      mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
            const permissionStore = usePermissionStore();
            toggleElement(el, permissionStore.hasPermission(binding.value));
      },

      updated(el: HTMLElement, binding: DirectiveBinding<string>) {
            const permissionStore = usePermissionStore();
            toggleElement(el, permissionStore.hasPermission(binding.value));
      }
};

export default permission;
