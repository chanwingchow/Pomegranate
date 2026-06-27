package com.chow.pomegranate.service.academic.affairs.api

import com.chow.pomegranate.service.academic.affairs.model.CourseSearchParam
import com.chow.pomegranate.service.academic.affairs.model.LoginParam
import com.chow.pomegranate.service.academic.affairs.model.LoginResult
import com.chow.pomegranate.service.foundation.Semester
import com.chow.pomegranate.tool.ocr.createCaptchaOcrEngine
import com.moondeveloper.ocr.OcrEngine
import io.ktor.utils.io.readText
import kotlinx.coroutines.runBlocking
import kotlinx.io.SystemLineSeparator
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail

/**
 * 教务系统测试。
 */
class AcademicAffairsTest {
    private lateinit var academicAffairs: AcademicAffairs
    private lateinit var captchaOcrEngine: OcrEngine
    private lateinit var username: String
    private lateinit var password: String

    @BeforeTest
    fun setUp() {
        academicAffairs = AcademicAffairs()
        captchaOcrEngine = createCaptchaOcrEngine {
            dataPath("src\\commonTest\\resources\\tessdata")
        }

        // 读取 local.properties 里面的用户配置
        // user.name=学号
        // user.password=密码
        val properties = SystemFileSystem.source(Path("../local.properties"))
            .buffered()
            .readText()
            .split(SystemLineSeparator)
            .filter { it.startsWith("user") }
            .associate {
                val (key, value) = it.split("=")
                key.trim() to value.trim()
            }

        username = properties["user.name"]!!
        password = properties["user.password"]!!
    }

    /**
     * 登录测试。
     */
    @Test
    fun login() = runBlocking {
        runWithAcademicAffairsLogin {}
    }

    /**
     * 获取课表测试。
     */
    @Test
    fun getTimetable() = runBlocking {
        runWithAcademicAffairsLogin {
            val semester = Semester.now()

            val timetable = academicAffairs.enrollment.getTimetable(semester)

            println(timetable)
        }
    }

    /**
     * 获取课程课表测试。
     */
    @Test
    fun getCourseTimetable() = runBlocking {
        runWithAcademicAffairsLogin {
            val semester = Semester.now()

            val courseTimetable = academicAffairs.enrollment.getCourseTimetable(semester)

            println(courseTimetable)
        }
    }

    /**
     * 获取考试安排测试。
     */
    @Test
    fun getExamSchedule() = runBlocking {
        runWithAcademicAffairsLogin {
            val semester = Semester.now()

            val examSchedule = academicAffairs.enrollment.getExamSchedule(semester)

            println(examSchedule)
        }
    }

    /**
     * 获取学业计划测试。
     */
    @Test
    fun getAcademicPlan() = runBlocking {
        runWithAcademicAffairsLogin {
            val academicPlan = academicAffairs.academic.getAcademicPlan()

            println(academicPlan)
        }
    }

    /**
     * 获取学业进度测试。
     */
    @Test
    fun getAcademicProgress() = runBlocking {
        runWithAcademicAffairsLogin {
            val academicProgress = academicAffairs.academic.getAcademicProgress()

            println(academicProgress)
        }
    }

    /**
     * 获取学业预警测试。
     */
    @Test
    fun getAcademicAlert() = runBlocking {
        runWithAcademicAffairsLogin {
            val academicAlert = academicAffairs.academic.getAcademicAlert()

            println(academicAlert)
        }
    }

    /**
     * 获取培养方案测试。
     */
    @Test
    fun getAcademicProgram() = runBlocking {
        runWithAcademicAffairsLogin {
            val academicProgram = academicAffairs.academic.getAcademicProgram()

            println(academicProgram)
        }
    }

    /**
     * 获取毕业审核测试。
     */
    @Test
    fun getGraduationAudit() = runBlocking {
        runWithAcademicAffairsLogin {
            val graduationAudit = academicAffairs.academic.getGraduationAudit()

            println(graduationAudit)
        }
    }

    /**
     * 进入选课系统入口测试。
     */
    @Test
    fun enterCourseSystemSession() = runBlocking {
        runWithEnterCourseSystem {}
    }

    /**
     * 获取选课学分概览测试。
     */
    @Test
    fun getCreditSummary() = runBlocking {
        runWithEnterCourseSystem {
            val creditSummary = academicAffairs.courseSystem.getCreditSummary()

            println(creditSummary)
        }
    }

    /**
     * 获取已选课程测试。
     */
    @Test
    fun getSelectedCourses() = runBlocking {
        runWithEnterCourseSystem {
            val selectedCourses = academicAffairs.courseSystem.getSelectedCourses()

            println(selectedCourses)
        }
    }

    /**
     * 获取退课记录测试。
     */
    @Test
    fun getCourseDropLogs() = runBlocking {
        runWithEnterCourseSystem {
            val courseDropLogs = academicAffairs.courseSystem.getCourseDropLogs()

            println(courseDropLogs)
        }
    }

    /**
     * 搜索选课系统课程测试。
     */
    @Test
    fun searchCourse() = runBlocking {
        runWithEnterCourseSystem {
            val params = CourseSearchParam.General()
            val response = academicAffairs.courseSystem.searchCourses(params)

            println(response)
        }
    }

    /**
     * 在进入选课系统的情况下运行 [block]。
     */
    private suspend inline fun runWithEnterCourseSystem(block: () -> Unit) {
        runWithAcademicAffairsLogin {
            val entrances = academicAffairs.courseSystem.getEntrances()

            println(entrances)

            if (entrances.isNotEmpty()) {
                // 进入选课系统
                academicAffairs.courseSystem.enterCourseSystem(entrances.first().id)
                return block()
            }

            fail()
        }
    }

    /**
     * 在登录的情况下运行 [block]。
     */
    private suspend inline fun runWithAcademicAffairsLogin(block: () -> Unit) {
        var count = 5

        while (count-- > 0) {
            // 获取验证码
            val bytes = academicAffairs.auth.getCaptcha()

            // 识别验证码
            val captcha = captchaOcrEngine.recognize(bytes)

            // 登录
            val param = LoginParam(
                username = username,
                password = password,
                captcha = captcha.fullText,
            )
            val loginResult = academicAffairs.auth.login(param)

            when (loginResult) {
                is LoginResult.Fail.CaptchaError -> {
                    continue
                }

                is LoginResult.Fail.Unknow -> {
                    fail()
                }

                is LoginResult.Success -> {
                    return try {
                        block()
                    } finally {
                        academicAffairs.auth.logout()
                    }
                }
            }
        }

        fail()
    }
}