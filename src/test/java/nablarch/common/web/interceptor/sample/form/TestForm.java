package nablarch.common.web.interceptor.sample.form;

import java.io.Serializable;

import nablarch.core.validation.ValidationTarget;

@SuppressWarnings("serial")
public class TestForm implements Serializable {
    private String str;
    private String[] strArray;
    private Boolean bool;
    private Long num;
    private TestDto testDto;
    private TestDto[] testDtoArray;

    private Integer sizeKey;
    private TestDto[] usingSizeKey;

    private TestDto privateDto;

    public TestDto getPrivateDto() {
        return privateDto;
    }

    @ValidationTarget
    private void setPrivateDto(TestDto privateDto) {
        this.privateDto = privateDto;
    }

    public String getReadOnly() {
        return str + num;
    }

    public String getStr() {
        return str;
    }
    public void setStr(String str) {
        this.str = str;
    }
    public String[] getStrArray() {
        return strArray;
    }
    public void setStrArray(String[] strArray) {
        this.strArray = strArray;
    }
    public Boolean getBool() {
        return bool;
    }
    public void setBool(Boolean bool) {
        this.bool = bool;
    }
    public Long getNum() {
        return num;
    }
    public void setNum(Long num) {
        this.num = num;
    }
    public TestDto getTestDto() {
        return testDto;
    }
    @ValidationTarget
    public void setTestDto(TestDto testDto) {
        this.testDto = testDto;
    }
    public TestDto[] getTestDtoArray() {
        return testDtoArray;
    }
    @ValidationTarget(size = 2)
    public void setTestDtoArray(TestDto[] testDtoArray) {
        this.testDtoArray = testDtoArray;
    }
    public Integer getSizeKey() {
        return sizeKey;
    }
    public void setSizeKey(Integer sizeKey) {
        this.sizeKey = sizeKey;
    }
    public TestDto[] getUsingSizeKey() {
        return usingSizeKey;
    }
    @ValidationTarget(sizeKey = "sizeKey")
    public void setUsingSizeKey(TestDto[] usingSizeKey) {
        this.usingSizeKey = usingSizeKey;
    }
}
