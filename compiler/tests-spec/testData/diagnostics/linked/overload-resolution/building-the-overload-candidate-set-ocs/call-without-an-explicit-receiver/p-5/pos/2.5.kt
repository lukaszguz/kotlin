// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -NOTHING_TO_INLINE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-278
 * PLACE: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 5 -> sentence 2
 * RELEVANT PLACES: overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 4 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 8 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 5
 * overload-resolution, building-the-overload-candidate-set-ocs, call-without-an-explicit-receiver -> paragraph 6 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: The overload candidate sets for each pair of implicit receivers: Implicitly imported extension callables
 */

// FILE: TestCase.kt
// TESTCASE NUMBER: 1
package testsCase1
import libPackageCase1.*
import libPackageCase1Explicit.emptyArray

class Case1(){

    fun case() {
        <!DEBUG_INFO_CALL("fqName: libPackageCase1.emptyArray; typeCall: extension function")!>emptyArray<Int>()<!>
    }
}

// FILE: Lib.kt
package libPackageCase1
import testsCase1.*

public fun <T> emptyArray(): Array<T> = TODO()
fun <T> Case1.emptyArray(): Array<T> = TODO()

// FILE: Lib.kt
package libPackageCase1Explicit

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: LibtestsPack.kt
package testsCase1

public fun <T> emptyArray(): Array<T> = TODO()


// FILE: TestCase.kt
// TESTCASE NUMBER: 2
package testsCase2
import libPackageCase2.*
import libPackageCase2Explicit.emptyArray

class Case2(){

    fun case() {
        <!DEBUG_INFO_CALL("fqName: libPackageCase2.emptyArray; typeCall: extension function")!>emptyArray<Int>()<!>
    }
}
class A {
    operator fun <T>invoke(): T = TODO()
}


// FILE: Lib.kt
package libPackageCase2
import testsCase2.*

val Case2.emptyArray: A
    get() = A()

public fun <T> emptyArray(): Array<T> = TODO()
fun <T> Case2.emptyArray(): Array<T> = TODO()

// FILE: Lib.kt
package libPackageCase2Explicit

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: LibtestsPack.kt
package testsCase2

public fun <T> emptyArray(): Array<T> = TODO()


// FILE: TestCase.kt
// TESTCASE NUMBER: 3
package testsCase3
import libPackageCase3.*
import libPackageCase3Explicit.emptyArray

class Case3(){

    fun case() {
        <!DEBUG_INFO_CALL("fqName: testsCase3.A.invoke; typeCall: variable&invoke")!>emptyArray<Int>()<!>
    }
}
class A {
    operator fun <T>invoke(): T = TODO()
}


// FILE: Lib.kt
package libPackageCase3
import testsCase3.*

val Case3.emptyArray: A
    get() = A()

fun <T> emptyArray(): Array<T> = TODO()
private fun <T> Case3.emptyArray(): Array<T> = TODO()

// FILE: Lib.kt
package libPackageCase3Explicit

public fun <T> emptyArray(): Array<T> = TODO()

// FILE: LibtestsPack.kt
package testsCase3

public fun <T> emptyArray(): Array<T> = TODO()

