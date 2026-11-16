package com.chow.pomegranate.session

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** Keychain 服务标识。 */
private const val SERVICE = "com.chow.pomegranate.secure_store"

actual fun createSecureStore(): SecureStore = IosSecureStore()

/**
 * 基于 Keychain 的安全存储。
 *
 * 使用 `kSecClassGenericPassword`，服务标识为 [SERVICE]，账户为业务键。
 * 条目在设备首次解锁后即可访问，重启应用仍可恢复登录凭据。
 */
@OptIn(ExperimentalForeignApi::class)
private class IosSecureStore : SecureStore {
    override fun put(
        key: String,
        value: String,
    ) {
        // 先删除同名条目，保证写入幂等
        remove(key)

        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

        memScoped {
            val query = createQuery(key)

            // NSData 需要显式桥接为 CoreFoundation 类型
            val dataRef = CFBridgingRetain(data)

            CFDictionaryAddValue(query, kSecValueData, dataRef)
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)

            SecItemAdd(query as CFDictionaryRef, null)

            CFBridgingRelease(dataRef)
            CFBridgingRelease(query)
        }
    }

    override fun get(key: String): String? = memScoped {
        val query = createQuery(key)

        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)

        CFBridgingRelease(query)

        if (status != errSecSuccess) return@memScoped null

        val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null

        NSString.create(data, NSUTF8StringEncoding) as String?
    }

    override fun remove(key: String) {
        memScoped {
            val query = createQuery(key)

            SecItemDelete(query as CFDictionaryRef)

            CFBridgingRelease(query)
        }
    }

    /** 返回包含类别、服务与账户的基础查询字典。 */
    private fun createQuery(key: String): CFMutableDictionaryRef {
        val query = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )!!

        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(SERVICE))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(key))

        return query
    }
}
