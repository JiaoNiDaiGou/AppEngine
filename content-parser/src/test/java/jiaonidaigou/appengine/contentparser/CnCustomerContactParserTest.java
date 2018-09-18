package jiaonidaigou.appengine.contentparser;

import jiaonidaigou.appengine.wiremodel.entity.Address;
import jiaonidaigou.appengine.wiremodel.entity.Customer;
import jiaonidaigou.appengine.wiremodel.entity.PhoneNumber;
import org.junit.Assert;
import org.junit.Test;

import static jiaonidaigou.appengine.contentparser.AnswerMatchers.atLeast;
import static jiaonidaigou.appengine.contentparser.AnswerMatchers.hasAnswerInTop;

public class CnCustomerContactParserTest {
    private final CnCustomerContactParser underTest = new CnCustomerContactParser();

    @Test
    public void testParseSimple() {
        String input = "新地址：上海市长宁区金钟路68弄剑河家苑5号1404，黄桦，13916608921";
        Answers<Customer> customerAnswers = underTest.parse(input);

        Customer expectedCustomer = Customer.newBuilder()
                .setName("黄桦")
                .setPhone(PhoneNumber.newBuilder().setCountryCode("86").setPhone("13916608921"))
                .addAddresses(Address.newBuilder()
                        .setRegion("上海市")
                        .setCity("上海市")
                        .setZone("长宁区")
                        .setAddress("金钟路68弄剑河家苑5号1404"))
                .build();

        Assert.assertThat(customerAnswers, hasAnswerInTop(3, atLeast(expectedCustomer, Conf.HIGH)));
    }
}
