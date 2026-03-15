import type { Directive, DirectiveBinding } from 'vue';
import { usePermissionStore } from '../stores/permission';

/**
 * 按钮权限指令
 * 用法：v-permission="'view'"
 */
export const permission: Directive = {
      mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
            const { value } = binding;
            const permissionStore = usePermissionStore();

            if (value && !permissionStore.hasPermission(value)) {
                  el.parentNode?.removeChild(el);
            }
      },

      updated(el: HTMLElement, binding: DirectiveBinding<string>) {
            const { value } = binding;
            const permissionStore = usePermissionStore();

            if (value && !permissionStore.hasPermission(value)) {
                  el.parentNode?.removeChild(el);
            }
      }
};

export default permission;
