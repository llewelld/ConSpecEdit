//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.04.03 at 02:41:26 AM BST 
//


package eu.aniketos;

import java.util.List;
import java.util.Vector;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for performType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="performType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="reaction" type="{}reactionType" maxOccurs="unbounded"/>
 *         &lt;element name="else" type="{}updateType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "performType", propOrder = {
    "reaction",
    "_else"
})
public class PerformType {

    @XmlElement(required = true)
    protected List<ReactionType> reaction = new Vector<ReactionType>();
    @XmlElement(name = "else")
    protected UpdateType _else;

    /**
     * Gets the value of the reaction property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the reaction property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReaction().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ReactionType }
     * 
     * 
     */
    public List<ReactionType> getReaction() {
        if (reaction == null) {
            reaction = new Vector<ReactionType>();
        }
        return this.reaction;
    }

    /**
     * Gets the value of the else property.
     * 
     * @return
     *     possible object is
     *     {@link UpdateType }
     *     
     */
    public UpdateType getElse() {
        return _else;
    }

    /**
     * Sets the value of the else property.
     * 
     * @param value
     *     allowed object is
     *     {@link UpdateType }
     *     
     */
    public void setElse(UpdateType value) {
        this._else = value;
    }

}
